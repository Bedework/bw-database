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
package org.bedework.database.jpa;

import org.bedework.base.exc.BedeworkException;
import org.bedework.base.exc.persist.BedeworkDatabaseException;
import org.bedework.base.exc.persist.BedeworkStaleStateException;
import org.bedework.database.db.DbSession;
import org.bedework.database.db.DbSessionFactoryProvider;
import org.bedework.database.db.InterceptorDbEntity;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.misc.Util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Query;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Convenience class to do the actual hibernate interaction. Intended for
 * one use only.
 *
 * @author Mike Douglass douglm@rpi.edu
 */
public class DbSessionImpl implements Logged, DbSession {
  protected EntityManager sess;
  protected transient EntityTransaction tx;
  protected boolean rolledBack;

  protected transient Query q;

  /** Exception from this session. */
  protected BedeworkException exc;

  private final SimpleDateFormat dateFormatter =
          new SimpleDateFormat("yyyy-MM-dd");

  @Override
  public DbSession init(final DbSessionFactoryProvider provider) {
    init(provider.getSessionFactory());
    return this;
  }

  @Override
  public DbSession init(final EntityManagerFactory factory) {
    try {
      sess = factory.createEntityManager();
      rolledBack = false;
    } catch (final Throwable t) {
      exc = new BedeworkDatabaseException(t);
      tx = null;  // not even started. Should be null anyway
      close();
    }
    return this;
  }

  @Override
  public EntityManager getSession() {
    return sess;
  }

  @Override
  public boolean isOpen() {
    try {
      if (sess == null) {
        return false;
      }
      return sess.isOpen();
    } catch (final Throwable t) {
      handleException(t);
      return false;
    }
  }

  @Override
  public BedeworkException getException() {
    return exc;
  }

  @Override
  public DbSession beginTransaction() {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      if (tx != null) {
        exc = new BedeworkDatabaseException("Transaction already started");
        throw exc;
      }

      tx = sess.getTransaction();
      rolledBack = false;
      if (tx == null) {
        exc = new BedeworkDatabaseException("Transaction not started");
        throw exc;
      }

      tx.begin();
    } catch (final BedeworkException be) {
      exc = be;
      throw be;
    } catch (final Throwable t) {
      exc = new BedeworkDatabaseException(t);
      throw exc;
    }
    return this;
  }

  @Override
  public boolean transactionStarted() {
    return tx != null;
  }

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
      if (t instanceof OptimisticLockException) {
        exc = new BedeworkStaleStateException(t);
        throw exc;
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

  @Override
  public void rollback() {
/*    if (exc != null) {
      // Didn't hear me last time?
      throw new BedeworkDatabaseException(exc);
    }
*/
    if (debug()) {
      debug("Enter rollback");
    }
    try {
      if ((tx != null) &&
          !rolledBack) {
        if (debug()) {
          debug("About to rollback");
        }
        tx.rollback();
        tx = null;
        sess.clear();
      }
    } catch (final Throwable t) {
      exc = new BedeworkDatabaseException(t);
      throw exc;
    } finally {
      rolledBack = true;
    }
  }

  @Override
  public boolean rolledback() {
    return rolledBack;
  }

  @Override
  public Timestamp getCurrentTimestamp(
          final Class<?> tableClass) {
    try {
      final List<?> l = sess.createQuery(
              "select current_timestamp() from " +
                      tableClass.getName()).getResultList();

      if (Util.isEmpty(l)) {
        return null;
      }

      return (Timestamp)l.get(0);
    } catch (final Throwable t) {
      handleException(t);
      return null;
    }
  }

  @Override
  public DbSession evict(final Object val) {
    if (exc != null) {
      // Didn't hear me last time?
      throw new BedeworkDatabaseException(exc);
    }

    try {
      sess.detach(val);
    } catch (final Throwable t) {
      handleException(t);
    }
    return this;
  }

  @Override
  public DbSession createQuery(final String s) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      q = sess.createQuery(s);
    } catch (final Throwable t) {
      handleException(t);
    }
    return this;
  }

  @Override
  public DbSession setString(final String parName,
                             final String parVal) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      q.setParameter(parName, parVal);
    } catch (final Throwable t) {
      handleException(t);
    }
    return this;
  }

  @Override
  public DbSession setBool(final String parName,
                           final boolean parVal) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      q.setParameter(parName, parVal);
    } catch (final Throwable t) {
      handleException(t);
    }
    return this;
  }

  @Override
  public DbSession setInt(final String parName,
                          final int parVal) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      q.setParameter(parName, parVal);
    } catch (final Throwable t) {
      handleException(t);
    }
    return this;
  }

  @Override
  public DbSession setLong(final String parName,
                           final long parVal) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      q.setParameter(parName, parVal);
    } catch (final Throwable t) {
      handleException(t);
    }
    return this;
  }

  @Override
  public DbSession setEntity(final String parName,
                             final Object parVal) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      q.setParameter(parName, parVal);
    } catch (final Throwable t) {
      handleException(t);
    }
    return this;
  }

  @Override
  public DbSession setParameterList(
          final String parName,
          final Collection<?> parVal) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      q.setParameter(parName, parVal);
    } catch (final Throwable t) {
      handleException(t);
    }
    return this;
  }

  @Override
  public DbSession setFirstResult(final int val) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      q.setFirstResult(val);
    } catch (final Throwable t) {
      handleException(t);
    }
    return this;
  }

  @Override
  public DbSession setMaxResults(final int val) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      q.setMaxResults(val);
    } catch (final Throwable t) {
      handleException(t);
    }
    return this;
  }

  @Override
  public Object getUnique() {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      return q.getSingleResult();
    } catch (final NoResultException ignored) {
      return null;
    } catch (final Throwable t) {
      handleException(t);
      return null;  // Don't get here
    }
  }

  @Override
  public List<?> getList() {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      final List<?> l = q.getResultList();

      if (l == null) {
        return new ArrayList<>();
      }

      return l;
    } catch (final Throwable t) {
      handleException(t);
      return null;  // Don't get here
    }
  }

  @Override
  public int executeUpdate() {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    if (q == null) {
      exc = new BedeworkDatabaseException("No query for execute update");
      throw exc;
    }

    try {
      return q.executeUpdate();
    } catch (final Throwable t) {
      handleException(t);
      return 0;  // Don't get here
    }
  }

  @Override
  public Object update(final Object obj) {
    return merge(obj);
  }

  @Override
  public Object merge(final Object obj) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      beforeUpdate(obj);
      final var merged = sess.merge(obj);
      afterUpdate(merged);

      return merged;
    } catch (final Throwable t) {
      handleException(t, obj);
      return null;
    }
  }

  @Override
  public Object get(final Class<?> cl,
                    final Serializable id) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      return sess.find(cl, id);
    } catch (final Throwable t) {
      handleException(t);
      return null;  // Don't get here
    }
  }

  @Override
  public Object get(final Class<?> cl, final int id) {
    return get(cl, Integer.valueOf(id));
  }

  @Override
  public DbSession add(final Object obj) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      beforeAdd(obj);
      sess.persist(obj);
      afterAdd(obj);
    } catch (final Throwable t) {
      handleException(t);
    }
    return this;
  }

  @Override
  public DbSession delete(final Object obj) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      beforeDelete(obj);

      // Do a merge to ensure not detached
      sess.remove(sess.merge(obj));
      afterDelete(obj);
    } catch (final Throwable t) {
      handleException(t);
    }
    return this;
  }

  @Override
  public DbSession refresh(final Object obj) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      sess.refresh(obj);
    } catch (final Throwable t) {
      handleException(t);
    }
    return this;
  }

  @Override
  public DbSession flush() {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    if (debug()) {
      debug("About to flush");
    }
    try {
      sess.flush();
    } catch (final Throwable t) {
      handleException(t);
    }
    return this;
  }

  @Override
  public DbSession clear() {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    if (debug()) {
      debug("About to flush");
    }
    try {
      sess.clear();
    } catch (final Throwable t) {
      handleException(t);
    }
    return this;
  }

  /**
   */
  @Override
  public DbSession close() {
    if (sess == null) {
      return this;
    }

    try {
      if (!rolledback() && (tx != null)) {
        tx.commit();
      }
    } catch (final Throwable t) {
      if (exc == null) {
        exc = new BedeworkException(t);
      }
    } finally {
      tx = null;
      if (sess != null) {
        try {
          sess.close();
        } catch (final Throwable ignored) {}
      }
    }

    sess = null;
    if (exc != null) {
      throw exc;
    }
    return this;
  }

  protected void handleException(final Throwable t) {
    handleException(t, null);
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

    if (t instanceof OptimisticLockException) {
      exc = new BedeworkStaleStateException(t);
      throw exc;
    }

    exc = new BedeworkDatabaseException(t);
    throw exc;
  }

  protected void beforeAdd(final Object o) {
    if (!(o instanceof final InterceptorDbEntity ent)) {
      return;
    }

    ent.beforeAdd();
  }

  protected void afterAdd(final Object o) {
    if (!(o instanceof final InterceptorDbEntity ent)) {
      return;
    }

    ent.afterAdd();
  }

  protected void beforeUpdate(final Object o) {
    if (!(o instanceof final InterceptorDbEntity ent)) {
      return;
    }

    ent.beforeUpdate();
  }

  protected void afterUpdate(final Object o) {
    if (!(o instanceof final InterceptorDbEntity ent)) {
      return;
    }

    ent.afterUpdate();
  }

  protected void beforeDelete(final Object o) {
    if (!(o instanceof final InterceptorDbEntity ent)) {
      return;
    }

    ent.beforeDeletion();
  }

  protected void afterDelete(final Object o) {
    if (!(o instanceof final InterceptorDbEntity ent)) {
      return;
    }

    ent.afterDeletion();
  }

  /** This is just in case we want to report rollback exceptions. Seems we're
   * likely to get one.
   *
   * @param t   Throwable from the rollback
   */
  public void rollbackException(final Throwable t) {
    error(t);
  }

  /* ====================================================
   *                   Logged methods
   * ==================================================== */

  protected final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
