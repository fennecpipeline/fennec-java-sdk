package org.fennec.sdk.model.commons;

import lombok.*;

/**
 * A link with an url, a name and a logo (url, svg, base64)
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Link {

    /**
     * The link name
     */
    private String name;

    /**
     * The link url
     */
    private String url;

    /**
     * The link logo (url, svg, base64)
     */
    private String logo;

}
