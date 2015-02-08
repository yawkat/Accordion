/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.accordion.distributed;

import at.yawk.accordion.netty.Connection;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class AsynchronousConnection implements Connection {
    private final Connection connection;
    private final Executor executor;

    private Consumer<Throwable> exceptionHandler = thr -> {};

    private void execute(Runnable task) {
        executor.execute(() -> {
            try {

                task.run();

            } catch (Throwable t) { exceptionHandler.accept(t); }
        });
    }

    @Override
    public void send(ByteBuf data) {
        execute(() -> connection.send(data));
    }

    @Override
    public void disconnect() {
        execute(connection::disconnect);
    }

    @Override
    public void setMessageHandler(Consumer<ByteBuf> listener) {
        execute(() -> {
            Consumer<ByteBuf> wrappedHandler =
                    message -> execute(
                            () -> listener.accept(message)
                    );

            connection.setMessageHandler(wrappedHandler);
        });
    }

    @Override
    public void setExceptionHandler(Consumer<Throwable> listener) {
        this.exceptionHandler = listener;
        execute(() -> {
            Consumer<Throwable> wrappedHandler =
                    throwable -> execute(
                            () -> listener.accept(throwable)
                    );

            connection.setExceptionHandler(wrappedHandler);
        });
    }

    @Override
    public void setDisconnectHandler(Runnable listener) {
        execute(() -> connection.setDisconnectHandler(() -> execute(listener)));
    }

    @Override
    public Map<String, Object> properties() {
        return connection.properties();
    }

    @Override
    public String toString() {
        return "AsynchronousConnection[" + connection + "]";
    }
}
