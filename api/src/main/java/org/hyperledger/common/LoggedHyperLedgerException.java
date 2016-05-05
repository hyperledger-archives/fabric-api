/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.common;

import org.slf4j.Logger;

/**
 * A HyperLedger exception, that is already logged
 */
public class LoggedHyperLedgerException extends HyperLedgerException {
    private LoggedHyperLedgerException(Throwable cause) {
        super(cause);
    }

    private LoggedHyperLedgerException(String message, Throwable cause) {
        super(message, cause);
    }

    private LoggedHyperLedgerException(String message) {
        super(message);
    }

    public static HyperLedgerException loggedInfo(Logger logger, String message) {
        logger.info(message);
        return new LoggedHyperLedgerException(message);
    }

    public static HyperLedgerException loggedError(Logger logger, String message) {
        logger.error(message);
        return new LoggedHyperLedgerException(message);
    }

    public static HyperLedgerException loggedWarn(Logger logger, String message) {
        logger.warn(message);
        return new LoggedHyperLedgerException(message);
    }

    public static HyperLedgerException loggedTrace(Logger logger, String message) {
        logger.trace(message);
        return new LoggedHyperLedgerException(message);
    }

    public static HyperLedgerException loggedInfo(Logger logger, String message, Throwable cause) {
        if (cause instanceof LoggedHyperLedgerException) {
            return (LoggedHyperLedgerException) cause;
        } else {
            logger.info(message);
            return new LoggedHyperLedgerException(message, cause);
        }
    }

    public static HyperLedgerException loggedError(Logger logger, String message, Throwable cause) {
        if (cause instanceof LoggedHyperLedgerException) {
            return (LoggedHyperLedgerException) cause;
        } else {
            logger.error(message, cause);
            return new LoggedHyperLedgerException(message, cause);
        }
    }

    public static HyperLedgerException loggedWarn(Logger logger, String message, Throwable cause) {
        if (cause instanceof LoggedHyperLedgerException) {
            return (LoggedHyperLedgerException) cause;
        } else {
            logger.warn(message, cause);
            return new LoggedHyperLedgerException(message, cause);
        }
    }

    public static HyperLedgerException loggedTrace(Logger logger, String message, Throwable cause) {
        if (cause instanceof LoggedHyperLedgerException) {
            return (LoggedHyperLedgerException) cause;
        } else {
            logger.trace(message, cause);
            return new LoggedHyperLedgerException(message, cause);
        }
    }

    public static HyperLedgerException loggedInfo(Logger logger, Throwable cause) {
        if (cause instanceof LoggedHyperLedgerException) {
            return (LoggedHyperLedgerException) cause;
        } else {
            logger.info(cause.getMessage(), cause);
            return new LoggedHyperLedgerException(cause.getMessage(), cause);
        }
    }

    public static HyperLedgerException loggedError(Logger logger, Throwable cause) {
        if (cause instanceof LoggedHyperLedgerException) {
            return (LoggedHyperLedgerException) cause;
        } else {
            logger.error(cause.getMessage(), cause);
            return new LoggedHyperLedgerException(cause.getMessage(), cause);
        }
    }

    public static HyperLedgerException loggedWarn(Logger logger, Throwable cause) {
        if (cause instanceof LoggedHyperLedgerException) {
            return (LoggedHyperLedgerException) cause;
        } else {
            logger.warn(cause.getMessage(), cause);
            return new LoggedHyperLedgerException(cause.getMessage(), cause);
        }
    }

    public static HyperLedgerException loggedTrace(Logger logger, Throwable cause) {
        if (cause instanceof LoggedHyperLedgerException) {
            return (LoggedHyperLedgerException) cause;
        } else {
            logger.trace(cause.getMessage(), cause);
            return new LoggedHyperLedgerException(cause.getMessage(), cause);
        }
    }
}
