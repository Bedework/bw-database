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
package org.bedework.database.openjpa;

import org.bedework.base.exc.BedeworkException;
import org.bedework.base.exc.persist.BedeworkDatabaseException;
import org.bedework.base.exc.persist.BedeworkStaleStateException;
import org.bedework.database.db.DbSession;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.misc.Util;

import org.apache.openjpa.persistence.OpenJPAEntityManager;

import java.io.Serializable;
import java.sql.Blob;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.OptimisticLockException;
import javax.persistence.Query;

/** Convenience class to do the actual hibernate interaction. Intended for
 * one use only.
 *
 * @author Mike Douglass douglm@rpi.edu
 */
public class DbSessionImpl implements Logged, DbSession {
  EntityManager sess;
  transient EntityTransaction tx;
  boolean rolledBack;

  transient Query q;

  /** Exception from this session. */
  BedeworkException exc;

  private final SimpleDateFormat dateFormatter =
          new SimpleDateFormat("yyyy-MM-dd");

  @Override
  public void init(final EntityManagerFactory factory) {
    try {
      sess = factory.createEntityManager();
      rolledBack = false;
    } catch (final Throwable t) {
      exc = new BedeworkDatabaseException(t);
      tx = null;  // not even started. Should be null anyway
      close();
    }
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
  public Throwable getException() {
    return exc;
  }

  @Override
  public void beginTransaction() {
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
//      if (tx != null &&
//          !tx.wasCommitted() &&
//          !tx.wasRolledBack()) {
        //if (getLogger().isDebugEnabled()) {
        //  getLogger().debug("About to comnmit");
        //}
      if ((tx != null) &&
              !rolledBack &&
              !tx.getRollbackOnly()) {
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
        obj = t.getClass().getClassLoader().loadClass("javax.persistence.OptimisticLockException");
      } catch (final ClassNotFoundException cnfe) {
        exc = new BedeworkDatabaseException(t);
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
      throw  new BedeworkDatabaseException(exc);
    }
*/
    if (getLogger().isDebugEnabled()) {
      getLogger().debug("Enter rollback");
    }
    try {
      if ((tx != null) &&
          !rolledBack) {
        if (getLogger().isDebugEnabled()) {
          getLogger().debug("About to rollback");
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
  public Blob getBlob(final byte[] val) {
    return Hibernate.getLobCreator(sess).createBlob(val);
  }

  @Override
  public void evict(final Object val) {
    if (exc != null) {
      // Didn't hear me last time?
      throw  new BedeworkDatabaseException(exc);
    }

    try {
      ((OpenJPAEntityManager)sess).evict(val);
    } catch (final Throwable t) {
      handleException(t);
    }
  }

  @Override
  public void createQuery(final String s) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      q = sess.createQuery(s);
    } catch (final Throwable t) {
      handleException(t);
    }
  }

  @Override
  public void cacheableQuery() {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      q.setCacheable(true);
    } catch (final Throwable t) {
      handleException(t);
    }
  }

  @Override
  public void setString(final String parName, final String parVal) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      q.setParameter(parName, parVal);
    } catch (final Throwable t) {
      handleException(t);
    }
  }

  @Override
  public void setBool(final String parName, final boolean parVal) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      q.setParameter(parName, parVal);
    } catch (final Throwable t) {
      handleException(t);
    }
  }

  @Override
  public void setInt(final String parName, final int parVal) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      q.setParameter(parName, parVal);
    } catch (final Throwable t) {
      handleException(t);
    }
  }

  @Override
  public void setLong(final String parName, final long parVal) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      q.setParameter(parName, parVal);
    } catch (final Throwable t) {
      handleException(t);
    }
  }

  @Override
  public void setEntity(final String parName, final Object parVal) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      q.setEntity(parName, parVal);
    } catch (final Throwable t) {
      handleException(t);
    }
  }

  @Override
  public void setParameterList(final String parName,
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
  }

  @Override
  public void setFirstResult(final int val) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      q.setFirstResult(val);
    } catch (final Throwable t) {
      handleException(t);
    }
  }

  @Override
  public void setMaxResults(final int val) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      q.setMaxResults(val);
    } catch (final Throwable t) {
      handleException(t);
    }
  }

  @Override
  public Object getUnique() {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      return q.getSingleResult();
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
  public void update(final Object obj) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      beforeSave(obj);
      sess.merge(obj);
      deleteSubs(obj);
    } catch (final Throwable t) {
      handleException(t);
    }
  }

  @Override
  public Object merge(Object obj) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      beforeSave(obj);

      obj = sess.merge(obj);
      deleteSubs(obj);

      return obj;
    } catch (final Throwable t) {
      handleException(t, obj);
      return null;
    }
  }

  @Override
  public void saveOrUpdate(final Object obj) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      beforeSave(obj);

      sess.merge(obj);
      deleteSubs(obj);
    } catch (final Throwable t) {
      handleException(t);
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
  public void save(final Object obj) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      beforeSave(obj);
      sess.persist(obj);
      deleteSubs(obj);
    } catch (final Throwable t) {
      handleException(t);
    }
  }

  @Override
  public void delete(final Object obj) {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    try {
      beforeDelete(obj);

      evict(obj);
      sess.remove(sess.merge(obj));
      deleteSubs(obj);
    } catch (final Throwable t) {
      handleException(t);
    }
  }

  @Override
  public void flush() {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    if (getLogger().isDebugEnabled()) {
      getLogger().debug("About to flush");
    }
    try {
      sess.flush();
    } catch (final Throwable t) {
      handleException(t);
    }
  }

  @Override
  public void clear() {
    if (exc != null) {
      // Didn't hear me last time?
      throw exc;
    }

    if (getLogger().isDebugEnabled()) {
      getLogger().debug("About to flush");
    }
    try {
      sess.clear();
    } catch (final Throwable t) {
      handleException(t);
    }
  }

  /**
   */
  @Override
  public void close() {
    if (sess == null) {
      return;
    }

//    throw  new BedeworkDatabaseException("XXXXXXXXXXXXXXXXXXXXXXXXXXXXX");/*
    try {
      if (!rolledback() && sess.isDirty()) {
        sess.flush();
      }
      if ((tx != null) && !rolledback()) {
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
//    */
  }

  private void handleException(final Throwable t) {
    handleException(t, null);
  }

  private void handleException(final Throwable t,
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

    /*
    if (t instanceof ConstraintViolationException) {
      throw new BedeworkConstraintViolationException(t);
    }*/

    exc = new BedeworkDatabaseException(t);
    throw exc;
  }

  private void beforeSave(final Object o) {
    if (!(o instanceof final VersionedDbEntity<?, ?> ent)) {
      return;
    }

    ent.beforeSave();
  }

  private void beforeDelete(final Object o) {
    if (!(o instanceof VersionedDbEntity)) {
      return;
    }

    final var ent = (VersionedDbEntity<?, ?>)o;

    ent.beforeDeletion();
  }

  private void deleteSubs(final Object o) {
    if (!(o instanceof final VersionedDbEntity<?, ?> ent)) {
      return;
    }

    final var subs = ent.getDeletedEntities();
    if (subs == null) {
      return;
    }

    for (final var sub: subs) {
      evict(sub);
      delete(sub);
    }
  }

  /** This is just in case we want to report rollback exceptions. Seems we're
   * likely to get one.
   *
   * @param t   Throwable from the rollback
   */
  private void rollbackException(final Throwable t) {
    error(t);
  }

  /* ====================================================================
   *                   Logged methods
   * ==================================================================== */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
