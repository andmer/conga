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

package io.fixprotocol.conga.session;

/**
 * Immutable event emitted when Session state changes
 * 
 * @author Don Mendelson
 *
 */
public class SessionEvent {

  private final String principal;
  private final byte [] sessionId;
  private final SessionState state;

  public SessionEvent(SessionState state, byte[] sessionId, String principal) {
    this.state = state;
    this.sessionId = sessionId;
    this.principal = principal;
  }

  public String getPrincipal() {
    return principal;
  }

  public byte[] getSessionId() {
    return sessionId;
  }

  public SessionState getState() {
    return state;
  }
  
}
