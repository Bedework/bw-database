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

import org.bedework.base.exc.persist.BedeworkDatabaseException;
import org.bedework.database.db.DbSession;
import org.bedework.database.db.DbSessionFactoryProvider;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.Configuration;

import java.util.List;
import java.util.Properties;

/** Convenience class to do the actual hibernate interaction.
 * Should be saved as a static object
 *
 * @author Mike Douglass bedework.org
 */
public class HibSessionFactoryProvider
        implements DbSessionFactoryProvider {
  private EntityManagerFactory sessionFactory;

  @Override
  public DbSessionFactoryProvider init(
          final List<String> props) {
    sessionFactory = getSessionFactory(props);
    return this;
  }

  @Override
  public EntityManagerFactory getSessionFactory() {
    return sessionFactory;
  }

  /**
   * @param props possibly null list of hibernate properties
   * @return the SessionFactory
   */
  public EntityManagerFactory getSessionFactory(
          final List<String> props) {
    return getSessionFactory(makeProperties(props));
  }


  @Override
  public EntityManagerFactory getSessionFactory(
          final Properties props) {
    /* Get a new hibernate session factory. This is configured from an
       * application resource hibernate.cfg.xml together with some run time values
       */
    try {
      final Configuration conf = new Configuration();

      if (props != null) {
        conf.addProperties(props);
      }

      conf.configure();

      return conf.buildSessionFactory();
    } catch (final Throwable t) {
      throw new BedeworkDatabaseException(t);
    }
  }

  @Override
  public DbSession getNewSession() {
    return new HibSessionImpl().init(this);
  }
}
