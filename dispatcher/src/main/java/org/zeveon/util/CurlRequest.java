package org.zeveon.util;

import lombok.Getter;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.zeveon.util.StringUtil.COLON;
import static org.zeveon.util.StringUtil.QUOT;

/**
 * @author Stanislav Vafin
 */
@Getter
public class CurlRequest {

    private final String command;

    private CurlRequest(CurlRequestBuilder builder) {
        this.command = builder.request.toString();
    }

    public static CurlRequestBuilder builder(String url) {
        return new CurlRequestBuilder(url);
    }

    public static class CurlRequestBuilder {

        private final StringBuilder request;

        private final String url;

        CurlRequestBuilder(String url) {
            this.request = new StringBuilder("curl");
            this.url = url;
        }

        /**
         * Show document info only
         */
        public CurlRequestBuilder head() {
            this.request.append(SPACE).append("-I");
            return this;
        }

        /**
         * Follow redirects
         */
        public CurlRequestBuilder location() {
            this.request.append(SPACE).append("-L");
            return this;
        }

        /**
         * Silent mode
         */
        public CurlRequestBuilder silent() {
            this.request.append(SPACE).append("-s");
            return this;
        }

        /**
         * Pass custom header(s) to server
         */
        public CurlRequestBuilder header(String headerKey, String headerValue) {
            this.request.append(SPACE)
                    .append("-H")
                    .append(SPACE)
                    .append(QUOT)
                    .append(headerKey)
                    .append(COLON)
                    .append(SPACE)
                    .append(headerValue)
                    .append(QUOT);
            return this;
        }

        /**
         * Maximum time allowed for connection
         */
        public CurlRequestBuilder connectTimeout(Integer seconds) {
            this.request.append(SPACE)
                    .append("--connect-timeout")
                    .append(SPACE)
                    .append(seconds);
            return this;
        }

        public CurlRequest build() {
            request.append(SPACE).append(url);
            return new CurlRequest(this);
        }
    }
}
