/*
 * Copyright (C) 2016 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.aggregate.parser;

import java.util.Random;
import java.util.UUID;
import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.persistence.ITaskLockType;
import org.opendatakit.common.persistence.PersistConsts;
import org.opendatakit.common.persistence.TaskLock;
import org.opendatakit.common.persistence.exception.ODKTaskLockException;
import org.opendatakit.common.security.User;
import org.opendatakit.common.web.CallingContext;

/**
 * Make datastore locks a little easier. NOT threadsafe.
 * Copied and modified from LockTemplate for the ODK 2.0
 *
 * @author mitchellsundt@gmail.com
 * @author the.dylan.price@gmail.com
 */
public class SubmissionLockTemplate {
  // At 4 tries and 250 initial backoff, the maximum amount of time a single
  // acquire or release can take is:
  // 250 + 500 + 1000 + 2000 = 3750
  private static final int TRIES = 4;
  private static final int INITIAL_MAX_BACKOFF = 250;
  private String formId;
  private Datastore ds;
  private User user;
  private String lockId;
  private long maxBackoffMs;
  private Random rand;

  public SubmissionLockTemplate(String formId, String instanceId, CallingContext cc) {
    if (instanceId == null || instanceId.length() == 0) {
      throw new IllegalArgumentException("instanceId ne peut pas être null ou vide");
    } else {
      this.formId = "submission|" + formId + "|" + Integer.toHexString(instanceId.hashCode() & 0xff);
    }

    this.ds = cc.getDatastore();
    this.user = cc.getCurrentUser();
    this.lockId = UUID.randomUUID().toString();
    this.maxBackoffMs = INITIAL_MAX_BACKOFF;
    this.rand = new Random();
  }

  public void acquire() throws ODKTaskLockException {
    TaskLock lock = ds.createTaskLock(user);
    boolean acquired = false;
    maxBackoffMs = INITIAL_MAX_BACKOFF;
    for (int i = 0; i < TRIES; i++) {
      if (lock.obtainLock(lockId, formId, SubmissionTaskLockType.MODIFICATION)) {
        acquired = true;
        break;
      } else {
        try {
          Thread.sleep(getNextBackoff());
        } catch (RuntimeException e) {
          throw new ODKTaskLockException(e);
        } catch (Exception e) {
          throw new ODKTaskLockException(e);
        }
      }
    }
    if (!acquired) {
      throw new ODKTaskLockException(String.format("Expiration du délai de verrouillage. "
          + "lockId: %s, formId: %s", lockId, formId, SubmissionTaskLockType.MODIFICATION));
    }
  }

  public void release() throws ODKTaskLockException {
    TaskLock lock = ds.createTaskLock(user);
    maxBackoffMs = INITIAL_MAX_BACKOFF;
    for (int i = 0; i < TRIES; i++) {
      if (lock.releaseLock(lockId, formId, SubmissionTaskLockType.MODIFICATION)) {
        break;
      } else {
        try {
          Thread.sleep(getNextBackoff());
        } catch (RuntimeException e) {
          throw new ODKTaskLockException(e);
        } catch (Exception e) {
          // just move on, this retry mechanism
          // is to make things nice
          break;
        }
      }
    }
  }

  private long getNextBackoff() {
    long backoff = (long) (rand.nextDouble() * maxBackoffMs);
    maxBackoffMs *= 2;
    return backoff;
  }

  private enum SubmissionTaskLockType implements ITaskLockType {
    MODIFICATION(66000, PersistConsts.MIN_SETTLE_MILLISECONDS);

    private long timeout;
    private long minSettleTime;


    private SubmissionTaskLockType(long timeout, long minSettle) {
      this.timeout = timeout;
      this.minSettleTime = minSettle;
    }

    @Override
    public long getLockExpirationTimeout() {
      return timeout;
    }

    @Override
    public String getName() {
      return name();
    }

    @Override
    public long getMinSettleTime() {
      return minSettleTime;
    }
  }

}
