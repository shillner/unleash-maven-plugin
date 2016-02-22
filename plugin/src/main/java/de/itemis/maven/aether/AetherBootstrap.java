package de.itemis.maven.aether;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;

public final class AetherBootstrap {
  private AetherBootstrap() {
  }

  public static RepositorySystem newRepositorySystem() {
    return RepositorySystemFactory.newRepositorySystem();
  }

  public static DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system,
      String localRepositoryPath) {
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

    // TODO setup session as described in settings.xml
    LocalRepository localRepo = new LocalRepository(localRepositoryPath);
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

    // TODO logger for repository access?
    /*
     * if (printConsoleOutput) { session.setTransferListener(new ConsoleTransferListener());
     * session.setRepositoryListener(new ConsoleRepositoryListener()); }
     */

    // uncomment to generate dirty trees
    // session.setDependencyGraphTransformer( null );

    return session;
  }

  public static List<RemoteRepository> newRemoteRepositories(RepositorySystem system, RepositorySystemSession session) {
    return new ArrayList<RemoteRepository>(Arrays.asList(newCentralRepository()));
  }

  public static RemoteRepository newCentralRepository() {
    return new RemoteRepository.Builder("nexus", "default", "http://nexus:8081/nexus/content/groups/public/").build();
  }
}