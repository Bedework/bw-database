package org.bedework.database.db;

import org.bedework.base.exc.BedeworkException;

import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class DbSessionFactoryProviderImpl
        implements DbSessionFactoryProvider {
  private static final String hibernateProvider =
          "org.bedework.database.hibernate.HibSessionFactoryProvider";
  private static final String openjpaProvider =
          "org.bedework.database.openjpa.OpenJPASessionFactoryProvider";

  private DbSessionFactoryProvider provider;

  @Override
  public DbSessionFactoryProvider init(final List<String> props) {
    final String sb = props.stream()
                           .map(p -> p + "\n")
                           .collect(Collectors.joining());

    try {
      final Properties dbProps = makeProperties(props);

      String ormName = dbProps.getProperty("org.bedework.orm");
      if (ormName == null) {
        ormName = System.getProperty("org.bedework.orm");
      }

      final String orm;

      if (ormName.equals("hibernate")) {
        orm = hibernateProvider;
      } else if (ormName.equals("openjpa")) {
        orm = openjpaProvider;
      } else {
        throw new BedeworkException("Unknown orm: " + ormName);
      }

      final ClassLoader loader = Thread.currentThread()
                                       .getContextClassLoader();
      final Class<?> cl = loader.loadClass(orm);
      final Object o = cl.getDeclaredConstructor()
                         .newInstance();
      provider = (DbSessionFactoryProvider)o;

      return provider.init(props);
    } catch (final BedeworkException be) {
      throw be;
    } catch (final Throwable t) {
      throw new BedeworkException(t);
    }
  }

  @Override
  public EntityManagerFactory getSessionFactory() {
    if (provider == null) {
      throw new BedeworkException("No DbSessionFactoryProvider available");
    }
    return provider.getSessionFactory();
  }

  @Override
  public EntityManagerFactory getSessionFactory(
          final List<String> props) {
    if (provider == null) {
      throw new BedeworkException("No DbSessionFactoryProvider available");
    }
    return provider.getSessionFactory(props);
  }

  @Override
  public EntityManagerFactory getSessionFactory(
          final Properties props) {
    if (provider == null) {
      throw new BedeworkException("No DbSessionFactoryProvider available");
    }
    return provider.getSessionFactory(props);
  }

  @Override
  public DbSession getNewSession() {
    if (provider == null) {
      throw new BedeworkException("No DbSessionFactoryProvider available");
    }
    return provider.getNewSession();
  }
}
