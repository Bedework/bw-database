package org.bedework.database.db;

import org.bedework.base.exc.BedeworkException;

import jakarta.persistence.EntityManagerFactory;

import java.io.StringReader;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/** Used to set up a factory and DbSession. Must have been
 * initialised before use.
 *
 */
public interface DbSessionFactoryProvider {
  /**
   * @param props possibly null db specific properties
   * @return possibly different factory provider
   */
  DbSessionFactoryProvider init(List<String> props);

  /**
   * @return factory set up by call to init
   */
  EntityManagerFactory getSessionFactory();

  /**
   * @param props possibly null db specific properties
   * @return the new EntityManagerFactory
   */
  EntityManagerFactory getSessionFactory(
          List<String> props);

  /**
   * @param props possibly null db specific properties
   * @return the new EntityManagerFactory
   */
  EntityManagerFactory getSessionFactory(
          Properties props);

  default Properties makeProperties(
          final List<String> props) {
    final String sb = props.stream()
                           .map(p -> p + "\n")
                           .collect(Collectors.joining());

    try {
      final Properties dbProps = new Properties();
      dbProps.load(new StringReader(sb));
      return dbProps;
    } catch (final Throwable t) {
      throw new BedeworkException(t);
    }
  }

  /**
   *
   * @return session created using the factory.
   */
  DbSession getNewSession();
}