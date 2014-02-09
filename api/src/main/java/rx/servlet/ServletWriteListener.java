/**
 * Copyright 2014 Jitendra Kotamraju.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.servlet;

import rx.Observer;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A servlet {@link WriteListener} that pushes Observable events that indicate
 * when HTTP response data can be written
 *
 * @author Jitendra Kotamraju
 */
class ServletWriteListener implements WriteListener {
    private static final Logger LOGGER = Logger.getLogger(ObservableServlet.class.getName());

    private final Observer<? super Void> observer;
    private final ServletOutputStream out;
    // Accessed by container thread, but assigned by some other thread (hence volatile)
    private volatile boolean unsubscribed;

    ServletWriteListener(Observer<? super Void> observer, final ServletOutputStream out) {
        this.observer = observer;
        this.out = out;
    }

    @Override
    public void onWritePossible() {
        do {
            observer.onNext(null);
        } while(!unsubscribed && out.isReady());
        if (LOGGER.isLoggable(Level.FINE) && !unsubscribed) {
            LOGGER.fine("Waiting for container to notify when HTTP response data can be written");
        }
    }

    @Override
    public void onError(Throwable t) {
        if (LOGGER.isLoggable(Level.WARNING)) {
            LOGGER.log(Level.WARNING, "Error while writing HTTP response data", t);
        }
        if (!unsubscribed) {
            observer.onError(t);
        }
    }

    void unsubscribe() {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Unsubscribing to writing HTTP response data");
        }
        unsubscribed = true;
    }
}
