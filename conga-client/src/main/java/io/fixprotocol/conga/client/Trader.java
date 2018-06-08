/*
 * Copyright 2018 FIX Protocol Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package io.fixprotocol.conga.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import io.fixprotocol.conga.buffer.BufferPool;
import io.fixprotocol.conga.buffer.BufferSupplier;
import io.fixprotocol.conga.buffer.RingBufferSupplier;
import io.fixprotocol.conga.client.io.ClientEndpoint;
import io.fixprotocol.conga.messages.Message;
import io.fixprotocol.conga.messages.MessageException;
import io.fixprotocol.conga.messages.MessageListener;
import io.fixprotocol.conga.messages.MutableMessage;
import io.fixprotocol.conga.messages.MutableNewOrderSingle;
import io.fixprotocol.conga.messages.MutableOrderCancelRequest;
import io.fixprotocol.conga.messages.MutableRequestMessageFactory;
import io.fixprotocol.conga.messages.ResponseMessageFactory;
import io.fixprotocol.conga.messages.sbe.SbeMutableRequestMessageFactory;
import io.fixprotocol.conga.messages.sbe.SbeResponseMessageFactory;

/**
 * Trader application sends orders and cancels to Exchange and receives executions
 * 
 * Session layer: WebSocket client
 * Presentation layer: Simple Binary Encoding (SBE)
 * @author Don Mendelson
 *
 */
public class Trader implements AutoCloseable {

  public static final String DEFAULT_HOST = "localhost";
  public static final String DEFAULT_PATH = "/trade";
  public static final int DEFAULT_PORT = 443;
  public static final int DEFAULT_TIMEOUT_SECONDS = 30;

  /**
   * @param args
   */
  public static void main(String[] args) {
    // TODO Auto-generated method stub

  }

  private final BufferSupplier bufferSupplier = new BufferPool();
  private final ClientEndpoint endpoint;
  private final ResponseMessageFactory responseFactory = new SbeResponseMessageFactory();
  private MessageListener messageListener = null;

  private final BiConsumer<String, ByteBuffer> incomingMessageConsumer = (source, buffer) -> {
    try {
      Message message = responseFactory.wrap(buffer);
      messageListener.onMessage(message);
    } catch (MessageException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  };
  private final MutableRequestMessageFactory requestFactory =
      new SbeMutableRequestMessageFactory(bufferSupplier);
  private final RingBufferSupplier ringBuffer;
  private int timeoutSeconds;

  public Trader(String host) throws URISyntaxException {
    this(host, DEFAULT_PORT, DEFAULT_PATH, DEFAULT_TIMEOUT_SECONDS);
  }

  public Trader(String host, int port, String path) throws URISyntaxException {
    this(host, port, path, DEFAULT_TIMEOUT_SECONDS);
  }

  public Trader(String host, int port, String path, int timeoutSeconds) throws URISyntaxException {
    this.timeoutSeconds = timeoutSeconds;
    this.ringBuffer = new RingBufferSupplier(incomingMessageConsumer);
    final URI uri = ClientEndpoint.createUri(host, port, path);
    this.endpoint = new ClientEndpoint(ringBuffer, uri, timeoutSeconds);
  }

  public void close() {
    try {
      endpoint.close();
      ringBuffer.stop();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * Returns an order message encoder
   * 
   * The encoder is thread-safe. That is, messages may be created on multiple threads concurrently.
   * However, a message must be populated on the same thread that invoked the create method.
   * 
   * @return a mutable order message
   */
  public MutableNewOrderSingle createOrder() {
    return requestFactory.getNewOrderSingle();
  }

  /**
   * Returns an order cancel message encoder
   * 
   * The encoder is thread-safe. That is, messages may be created on multiple threads concurrently.
   * However, a message must be populated on the same thread that invoked the create method.
   * 
   * @return a mutable order cancel message
   */
  public MutableOrderCancelRequest createOrderCancelRequest() {
    return requestFactory.getOrderCancelRequest();
  }

  /**
   * Send an order or cancel request
   * 
   * @param message
   * @throws TimeoutException if the operation fails to complete in a timeout period
   * @throws InterruptedException if the current thread is interrupted
   * @throws IOException if an I/O error occurs
   */
  public void send(MutableMessage message) throws Exception {
    try {
      CompletableFuture<ByteBuffer> future = endpoint.send(message.toBuffer());
      future.get(timeoutSeconds, TimeUnit.SECONDS);
    } finally {
      message.release();
    }
  }

  public void open(MessageListener listener) throws Exception {
    this.messageListener = listener;
    ringBuffer.start();
    endpoint.open();
  }
}