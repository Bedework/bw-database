/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.database.hibernate;

import org.bedework.base.exc.BedeworkException;
import org.bedework.base.exc.persist.BedeworkConstraintViolationException;
import org.bedework.base.exc.persist.BedeworkDatabaseException;
import org.bedework.base.exc.persist.BedeworkStaleStateException;
import org.bedework.database.jpa.DbSessionImpl;

import org.hibernate.StaleStateException;
import org.hibernate.exception.ConstraintViolationException;

import jakarta.persistence.OptimisticLockException;

/** Class to do the actual database interaction.
 *
 * @author Mike Douglass douglm@rpi.edu
 */
public class HibSessionImpl extends DbSessionImpl {
  @Override
  public void commit() {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      if (tx != null) {
        tx.commit();
      }

      tx = null;
    } catch (final Throwable t) {
      if (t instanceof StaleStateException) {
        exc = new BedeworkStaleStateException(t);
        throw exc;
      }

      if (debug()) {
        error("---------- Exception in commit ---------");
        error(t);
      }

      final Class<?> obj;
      try {
        obj = t.getClass().getClassLoader().loadClass(
                "jakarta.persistence.OptimisticLockException");
      } catch (final ClassNotFoundException cnfe) {
        exc = new BedeworkDatabaseException(cnfe);
        throw exc;
      }
      if (t.getClass().isAssignableFrom(obj)) {
        exc = new BedeworkStaleStateException(t);
        throw exc;
      }

      exc = new BedeworkDatabaseException(t);
      throw exc;
    }
  }

  protected void handleException(final Throwable t,
                               final Object o) {
    try {
      if (debug()) {
        debug("handleException called");
        if (o != null) {
          debug(o.toString());
        }
        error(t);
      }
    } catch (final Throwable ignored) {}

    try {
      if (tx != null) {
        try {
          tx.rollback();
        } catch (final Throwable t1) {
          rollbackException(t1);
        }
        tx = null;
      }
    } finally {
      try {
        sess.close();
      } catch (final Throwable ignored) {}
      sess = null;
    }

    if (t instanceof final BedeworkException be) {
      exc = be;
      throw exc;
    }

    if (t instanceof StaleStateException) {
      exc = new BedeworkStaleStateException(t);
      throw exc;
    }

    if (t instanceof OptimisticLockException) {
      exc = new BedeworkStaleStateException(t);
      throw exc;
    }

    if (t instanceof ConstraintViolationException) {
      exc = new BedeworkConstraintViolationException(t);
      throw exc;
    }

    exc = new BedeworkDatabaseException(t);
    throw exc;
  }
}
