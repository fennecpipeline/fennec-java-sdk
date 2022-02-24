package org.fennec.sdk.pipeline;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.fennec.sdk.error.Fail;
import org.fennec.sdk.model.commons.Deployment;
import org.fennec.sdk.model.commons.DeploymentType;
import org.fennec.sdk.model.commons.Link;
import org.fennec.sdk.pipeline.model.ExecStage;
import org.fennec.sdk.pipeline.model.SimpleStageHandler;
import org.slf4j.MDC;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class Pipeline {

    public static final String DEPLOY_TO = "Deploy to ";
    private static Pipeline PIPELINE;

    private final StageEventPublisher eventPublisher = new StageEventPublisher();

    private final PipelineContext pipelineContext = new PipelineContext();

    /**
     * The runnable to execute when pipeline fail
     */
    private final Runnable failPipeline;

    public static final Pipeline pipeline() {
        if (PIPELINE == null) {
            PIPELINE = new Pipeline(() -> System.exit(1));
        }
        return PIPELINE;
    }

    public static final Pipeline configure(Runnable failPipeline) {
        PIPELINE = new Pipeline(failPipeline);
        return PIPELINE;
    }

    /**
     * Using static import rename the pipeline
     *
     * <pre>
     *
     * rename("New name");
     *
     * </pre>
     *
     * @param newName the new pipeline display name
     */
    public static void rename(String newName) {
        pipeline().renamePipeline(newName);
    }

    /**
     * Using static import add links
     *
     * <pre>
     *
     * links(Arrays.asList(new Link("name", "url", "logo")));
     *
     * </pre>
     *
     * @param links the links to add
     */
    public static void links(List<Link> links) {
        pipeline().addLinks(links);
    }

    /**
     * Using static import add link
     *
     * <pre>
     *
     * link(new Link("name", "url", "logo"));
     *
     * </pre>
     *
     * @param link the link to add
     */
    public static void link(Link link) {
        pipeline().addLink(link);
    }

    /**
     * Using static import add link
     *
     * <pre>
     *
     * link("name", "url", "logo");
     *
     * </pre>
     *
     * @param name the link name
     * @param url the link url
     * @param logo the link logo
     */
    public static void link(String name, String url, String logo) {
        pipeline().addLink(name, url, logo);
    }

    /**
     * Using static import can start a stage with<br>
     *
     * <pre>
     *
     * stage("Stage name", (context) -&gt; {
     *   // logic here
     * });
     *
     * </pre>
     * <p>
     * or
     *
     * <pre>
     *
     * stage("Stage name", new StageOne());
     *
     * </pre>
     *
     * @param name    the stage name
     * @param handler the execution handler
     */
    public static void stage(String name, SimpleStageHandler handler) {
        pipeline().runSimpleStage(name, handler);
    }

    /**
     * Using static import can start a stage with<br>
     *
     * <pre>
     *
     * parallel("Parallel Name", Map.of(
     *    "Stage 1a", (context) -&gt; {
     *    },
     *    "Stage 1b", (context) -&gt; {
     *    });
     * </pre>
     *
     * @param parallelName   the parallel name
     * @param parallelStages the stages to execute in parallel
     */
    public static void parallel(String parallelName, Map<String, SimpleStageHandler> parallelStages) {
        pipeline().execParallel(parallelName, parallelStages);
    }

    /**
     * Wrap a deployment in a stage
     *
     * <pre>
     *
     * deploy("staging", (context) -&gt; {
     *   // logic here
     * });
     *
     * </pre>
     *
     * @param target            the deployment target
     * @param deploymentHandler the deployment handler
     */
    public static void deploy(String target, SimpleStageHandler deploymentHandler) {
        pipeline().execDeployment(target, deploymentHandler);
    }

    /**
     * Wrap a deployment in a stage. Perform roll-back in case deployment fail
     *
     * <pre>
     *
     * deploy("staging",
     *     (context) -&gt; {
     *       // logic here
     *     },
     *     (context) -&gt; {
     *       // rollback logic goes here
     *     });
     *
     * </pre>
     *
     * @param target            the deployment target
     * @param deploymentHandler the deployment handler
     * @param rollbackHandler   the rollback handler in case of issue
     */
    public static void deploy(String target, SimpleStageHandler deploymentHandler, SimpleStageHandler rollbackHandler) {
        pipeline().execDeployment(target, deploymentHandler, rollbackHandler);
    }

    /**
     * Perform deployment in parallel. This wrap in parallel stages
     *
     * <pre>
     *
     * deploy("staging", "region", Map.of(
     *    "eu-west-1", (context) -&gt; {
     *      // deployment staging to eu-west-1
     *    },
     *    "eu-east-1", (context) -&gt; {
     *       // deployment staging to eu-east-1
     *    });
     * </pre>
     *
     * @param target              the deployment target
     * @param indicator           will be mapped with the key of the handler.
     * @param parallelDeployments the parallel deployment handlers
     */
    public static void deploy(String target, String indicator, Map<String, SimpleStageHandler> parallelDeployments) {
        pipeline().execDeployment(target, indicator, parallelDeployments);
    }

    /**
     * Perform deployment in parallel. Perform parallel roll-back in case one of the load fail. This wrap in parallel
     * stages
     *
     * <pre>
     *
     * deploy("staging", "region",
     *     Map.of(
     *         "eu-west-1", (context) -&gt; {
     *           // deployment staging to eu-west-1
     *         },
     *         "eu-east-1", (context) -&gt; {
     *           // deployment staging to eu-east-1
     *         }),
     *     Map.of(
     *         "eu-west-1", (context) -&gt; {
     *           // Rollback staging eu-west-1
     *         },
     *         "eu-east-1", (context) -&gt; {
     *           // Rollback staging to eu-east-1
     *         }));
     *
     * </pre>
     *
     * @param target              the deployment target
     * @param indicator           will be mapped with the key of the handler.
     * @param parallelDeployments the parallel deployment handlers
     * @param parallelRollbacks   the parallel rollback handlers in case one fail
     */
    public static void deploy(String target, String indicator, Map<String, SimpleStageHandler> parallelDeployments,
            Map<String, SimpleStageHandler> parallelRollbacks) {
        pipeline().execDeployment(target, indicator, parallelDeployments, parallelRollbacks);
    }

    /**
     * Rename the pipeline
     *
     * @param newName the new pipeline display name
     */
    public void renamePipeline(String newName) {
        eventPublisher.updateJob(newName, Collections.emptyList());
    }

    /**
     * Add links
     *
     * @param links the links to add to job
     */
    public void addLinks(List<Link> links) {
        eventPublisher.updateJob(null, links);
    }

    /**
     * Add link
     *
     * @param link the links to add to job
     */
    public void addLink(Link link) {
        eventPublisher.updateJob(null, Arrays.asList(link));
    }

    /**
     * Add link
     *
     * @param name the link name
     * @param url  the link url
     * @param logo the logo url
     */
    public void addLink(String name, String url, String logo) {
        eventPublisher.updateJob(null, Arrays.asList(Link.builder().name(name).url(url).logo(logo).build()));
    }

    /**
     * Runs a stage and return current pipeline. Allows to chain stages<br>
     *
     * <pre>
     *
     * pipeline()
     *     .execStage("Stage one", (context) -&gt; {
     *       // logic here
     *     })
     *     .execStage("Stage two", (context) -&gt; {
     *       // logic here
     *     });
     *
     * </pre>
     * <p>
     * or
     *
     * <pre>
     *
     * pipeline()
     *     .execStage("Stage one", new StageOne())
     *     .execStage("Stage two", new StageTwo());
     *
     * </pre>
     *
     * @param name    the stage name
     * @param handler the execution handler
     * @return the pipeline
     */
    public Pipeline runSimpleStage(String name, SimpleStageHandler handler) {
        if (!runSimpleStage(new ExecStage(name, handler))) {
            failPipeline.run();
        }
        return this;
    }

    /**
     * Runs parallel stages and return current pipeline. Allows to chain stages<br>
     *
     * <pre>
     *
     * pipeline()
     * .parallel("Parallel Name", Map.of(
     *    "Stage 1a", (context) -&gt; {
     *    },
     *    "Stage 1b", (context) -&gt; {
     *    });
     * </pre>
     *
     * @param parallelName   the parallel name
     * @param parallelStages the stages to execute in parallel
     * @return the pipeline
     */
    public Pipeline execParallel(String parallelName, Map<String, SimpleStageHandler> parallelStages) {
        if (!runParallelStages(parallelStages
                .entrySet()
                .stream()
                .map(e -> new ExecStage(e.getKey(), parallelName, e.getValue()))
                .collect(Collectors.toList()))) {
            failPipeline.run();
        }
        return this;
    }

    /**
     * Wrap a deployment in a stage.
     *
     * <pre>
     *
     * pipeline().execDeployment("staging",
     *     (context) -&gt; {
     *       // logic here
     *     });
     *
     * </pre>
     *
     * @param target            the deployment target
     * @param deploymentHandler the deployment handler
     * @return the pipeline
     */
    public Pipeline execDeployment(String target, SimpleStageHandler deploymentHandler) {
        if (!runSimpleStage(new ExecStage(DEPLOY_TO + target,
                new Deployment(target, DeploymentType.LOAD),
                deploymentHandler))) {
            failPipeline.run();
        }
        return this;
    }

    /**
     * Wrap a deployment in a stage. Perform roll-back in case deployment fail
     *
     * <pre>
     *
     * pipeline().execDeployment("staging",
     *     (context) -&gt; {
     *       // logic here
     *     },
     *     (context) -&gt; {
     *       // rollback logic goes here
     *     });
     *
     * </pre>
     *
     * @param target            the deployment target
     * @param deploymentHandler the deployment handler
     * @param rollbackHandler   the rollback handler in case of issue
     * @return the pipeline
     */
    public Pipeline execDeployment(String target, SimpleStageHandler deploymentHandler,
            SimpleStageHandler rollbackHandler) {
        boolean success = runSimpleStage(new ExecStage(DEPLOY_TO + target,
                new Deployment(target, DeploymentType.LOAD),
                deploymentHandler));
        if (!success) {
            runSimpleStage(new ExecStage("Rollback " + target,
                    new Deployment(target, DeploymentType.ROLLBACK),
                    rollbackHandler));
            failPipeline.run();
        }
        return this;
    }

    /**
     * Perform deployment in parallel. This wrap in parallel stages
     *
     * <pre>
     *
     * pipeline()
     * .execDeployment("staging", "region", Map.of(
     *    "eu-west-1", (context) -&gt; {
     *      // deployment staging to eu-west-1
     *    },
     *    "eu-east-1", (context) -&gt; {
     *       // deployment staging to eu-east-1
     *    });
     * </pre>
     *
     * @param target              the deployment target
     * @param indicator           will be mapped with the key of the handler.
     * @param parallelDeployments the parallel deployment handlers
     * @return the pipeline
     */
    public Pipeline execDeployment(String target, String indicator,
            Map<String, SimpleStageHandler> parallelDeployments) {
        List<ExecStage> stages = parallelDeployments
                .entrySet()
                .stream()
                .map(e -> new ExecStage(String.format("Deploy to %s (%s)", target, e.getKey()),
                        DEPLOY_TO + target,
                        new Deployment(target, indicator, e.getKey(), DeploymentType.LOAD),
                        e.getValue()))
                .collect(Collectors.toList());
        if (!runParallelStages(stages)) {
            failPipeline.run();
        }
        return this;
    }

    /**
     * Perform deployment in parallel. Perform parallel roll-back in case one of the load fail. This wrap in parallel
     * stages
     *
     * <pre>
     *
     * pipeline()
     *     .execDeployment("staging", "region",
     *         Map.of(
     *             "eu-west-1", (context) -&gt; {
     *               // deployment staging to eu-west-1
     *             },
     *             "eu-east-1", (context) -&gt; {
     *               // deployment staging to eu-east-1
     *             }),
     *         Map.of(
     *             "eu-west-1", (context) -&gt; {
     *               // Rollback staging eu-west-1
     *             },
     *             "eu-east-1", (context) -&gt; {
     *               // Rollback staging to eu-east-1
     *             }));
     *
     * </pre>
     *
     * @param target              the deployment target
     * @param indicator           will be mapped with the key of the handler.
     * @param parallelDeployments the parallel deployment handlers
     * @param parallelRollbacks   the parallel rollback handlers in case one fail
     * @return the pipeline
     */
    public Pipeline execDeployment(String target, String indicator, Map<String, SimpleStageHandler> parallelDeployments,
            Map<String, SimpleStageHandler> parallelRollbacks) {

        if (!parallelDeployments.keySet().containsAll(parallelRollbacks.keySet()) || parallelDeployments
                .keySet()
                .size() != parallelRollbacks.keySet().size()) {
            // Create a failing stage
            runSimpleStage(new ExecStage(String.format("Deploy to %s", target), (context) -> {
                Fail.fail(String.format("Parallel rollback (%s) must contains the same keys as Parallel deployment (%s)",
                        parallelDeployments.keySet().stream().sorted().collect(Collectors.toList()),
                        parallelRollbacks.keySet().stream().sorted().collect(Collectors.toList())));
            }));
            failPipeline.run();
        }

        List<ExecStage> stages = parallelDeployments
                .entrySet()
                .stream()
                .map(e -> new ExecStage(String.format("Deploy to %s (%s)", target, e.getKey()),
                        DEPLOY_TO + target,
                        new Deployment(target, indicator, e.getKey(), DeploymentType.LOAD),
                        e.getValue()))
                .collect(Collectors.toList());
        boolean success = runParallelStages(stages);
        if (!success) {
            List<ExecStage> rollbackStages = parallelRollbacks
                    .entrySet()
                    .stream()
                    .map(e -> new ExecStage(String.format("Rollback %s (%s)", target, e.getKey()),
                            "Rollback " + target,
                            new Deployment(target, indicator, e.getKey(), DeploymentType.ROLLBACK),
                            e.getValue()))
                    .collect(Collectors.toList());
            runParallelStages(rollbackStages);
            failPipeline.run();
        }
        return this;
    }

    /**
     * Execute parallel stage
     *
     * @param execStages the list of exec stages
     * @return true if stages are success
     */
    private boolean runParallelStages(List<ExecStage> execStages) {
        try {
            List<CompletableFuture<Boolean>> completableFutures = new ArrayList<CompletableFuture<Boolean>>();
            // Use a for loop as stream force sync
            for (ExecStage execStage : execStages) {
                completableFutures.add(CompletableFuture.supplyAsync(() -> runSimpleStage(execStage)));
            }
            return completableFutures.stream().map(CompletableFuture::join).allMatch(Boolean::valueOf);
        } catch (Exception e) {
            return false;
        } finally {
            MDC.clear();
        }
    }

    /**
     * Execute a stage
     *
     * @param stage the execution stage
     * @return true if success
     */
    private boolean runSimpleStage(ExecStage stage) {
        Map<String, String> mdcContextMap = Optional.ofNullable(MDC.getCopyOfContextMap()).orElse(new HashMap<>());
        mdcContextMap.put(PipelineConstants.STAGE_NAME, stage.getName());
        if (stage.getParallel() != null) {
            mdcContextMap.put(PipelineConstants.STAGE_NAME, stage.getName());
        }
        MDC.setContextMap(mdcContextMap);
        StageContext context = new StageContextDefaultImpl(stage.getName(),
                stage.getParallel(),
                pipelineContext.getVersion());
        eventPublisher.start(stage.getName(), stage.getParallel(), stage.getDeployment());
        try {
            stage.getHandler().run(context);
            eventPublisher.end(stage.getName(), context.getTestResults());
            if (context.getVersion() != null) {
                pipelineContext.setVersion(context.getVersion());
            }
            return true;
        } catch (Exception e) {
            eventPublisher.error(stage.getName(), e, context.getTestResults());
            return false;
        } finally {
            MDC.clear();
        }
    }
}
