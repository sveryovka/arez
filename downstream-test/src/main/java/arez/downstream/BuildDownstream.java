package arez.downstream;

import gir.Gir;
import gir.GirException;
import gir.delta.Patch;
import gir.git.Git;
import gir.io.Exec;
import gir.io.FileUtil;
import gir.ruby.Buildr;
import gir.ruby.Ruby;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Stream;

@SuppressWarnings( "Duplicates" )
public final class BuildDownstream
{
  public static void main( final String[] args )
  {
    try
    {
      run();
    }
    catch ( final Exception e )
    {
      System.err.println( "Failed command." );
      e.printStackTrace( System.err );
      System.exit( 42 );
    }
  }

  private static void run()
    throws Exception
  {
    Gir.go( () -> {
      final String version = WorkspaceUtil.getVersion();
      final Path workingDirectory = WorkspaceUtil.setupWorkingDirectory();
      Stream.of( "arez-browserlocation",
                 "arez-dom",
                 "arez-promise",
                 "arez-mediaquery",
                 "arez-networkstatus",
                 "arez-spytools",
                 "arez-ticker",
                 "arez-timeddisposer",
                 "arez-when" )
        .forEach( project -> FileUtil.inDirectory( workingDirectory, () -> {
          Gir.messenger().info( "Cloning " + project + " into " + workingDirectory );
          Git.clone( "https://github.com/arez/" + project + ".git", project );
          final Path appDirectory = workingDirectory.resolve( project );
          FileUtil.inDirectory( appDirectory, () -> {
            Git.fetch();
            Git.resetBranch();
            Git.checkout();
            Git.pull();
            Git.deleteLocalBranches();
            Gir.messenger().info( "Processing branch master." );

            Git.checkout( "master" );
            Git.clean();
            final String newBranch = "master-ArezUpgrade-" + version;

            Git.checkout( newBranch, true );
            if ( Git.remoteTrackingBranches().contains( "origin/" + newBranch ) )
            {
              Git.pull();
            }
            Git.clean();

            Gir.messenger().info( "Building branch master prior to modifications." );
            boolean initialBuildSuccess = false;
            try
            {
              WorkspaceUtil.customizeBuildr( appDirectory );
              Ruby.buildr( "clean", "package", "PRODUCT_VERSION=", "PREVIOUS_PRODUCT_VERSION=" );
              initialBuildSuccess = true;
            }
            catch ( final GirException e )
            {
              Gir.messenger().info( "Failed to build branch 'master' before modifications.", e );
            }

            Git.resetBranch();
            Git.clean();

            final String group = "org.realityforge.arez";
            final Function<String, String> patchFunction1 = c -> Buildr.patchMavenCoordinates( c, group, version );
            final boolean patched =
              Patch.patchAndAddFile( appDirectory, appDirectory.resolve( "build.yaml" ), patchFunction1 );

            if ( patched )
            {
              final String message = "Update the '" + group + "' dependencies to version '" + version + "'";
              final Function<String, String> patchFunction = c -> {
                if ( c.contains( "### Unreleased\n\n#" ) )
                {
                  return c.replace( "### Unreleased\n\n", "### Unreleased\n\n* " + message + "\n\n" );
                }
                else
                {
                  return c.replace( "### Unreleased\n\n", "### Unreleased\n\n* " + message + "\n" );
                }
              };
              Patch.patchAndAddFile( appDirectory, appDirectory.resolve( "CHANGELOG.md" ), patchFunction );
              Git.commit( message );
            }
            Gir.messenger().info( "Building branch master after modifications." );
            WorkspaceUtil.customizeBuildr( appDirectory );

            try
            {
              /*
               * The process will run through the steps required for a release right up to tagging the source repository.
               * A subsequent call from within release.rake will complete the release process.
               */
              Ruby.buildr( "perform_release",
                           "LAST_STAGE=TagProject",
                           "PRODUCT_VERSION=",
                           "PREVIOUS_PRODUCT_VERSION=" );
              Git.checkout( "master" );
              Exec.system( "git", "merge", newBranch );
              Git.deleteBranch( newBranch );
            }
            catch ( final GirException e )
            {
              if ( !initialBuildSuccess )
              {
                Gir.messenger().error( "Failed to build branch 'master' before modifications " +
                                       "but branch also failed prior to modifications.", e );
              }
              else
              {
                Gir.messenger().error( "Failed to build branch 'master' after modifications.", e );
              }
              throw e;
            }
          } );
        } ) );
    } );
  }
}
