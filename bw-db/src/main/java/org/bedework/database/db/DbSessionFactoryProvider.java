package org.bedework.database.db;

import jakarta.persistence.EntityManagerFactory;

import java.util.List;

/** Can be used to set up a DbSession. Must have been
 * initialised before use.
 *
 */
public interface DbSessionFactoryProvider {
  /**
   *
   * @param props db specific properties
   */
  void init(List<String> props);

  /**
   *
   * @return factory set up by call to init
   */
  EntityManagerFactory getSessionFactory();
}
