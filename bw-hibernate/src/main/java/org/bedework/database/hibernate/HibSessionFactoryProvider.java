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
import org.bedework.database.db.DbSessionFactoryProvider;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.io.StringReader;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/** Convenience class to do the actual hibernate interaction.
 * Should be saved as a static object
 *
 * @author Mike Douglass bedework.org
 */
public class HibSessionFactoryProvider
        implements DbSessionFactoryProvider {
  private EntityManagerFactory sessionFactory;

  @Override
  public void init(
          final List<String> props) {
    sessionFactory = getSessionFactory(props);
  }

  @Override
  public EntityManagerFactory getSessionFactory() {
    return sessionFactory;
  }

  /**
   * @param hibProps possibly null list of hibernate properties
   * @return the SessionFactory
   */
  public static SessionFactory getSessionFactory(
          final List<String> hibProps) {
    /* Get a new hibernate session factory. This is configured from an
       * application resource hibernate.cfg.xml together with some run time values
       */
    try {
      final Configuration conf = new Configuration();

      if (hibProps != null) {
        final String sb = hibProps.stream().map(p -> p + "\n")
                                  .collect(Collectors.joining());

        final Properties hprops = new Properties();
        hprops.load(new StringReader(sb));

        conf.addProperties(hprops);
      }

      conf.configure();

      return conf.buildSessionFactory();
    } catch (final Throwable t) {
      throw new BedeworkDatabaseException(t);
    }
  }
}
