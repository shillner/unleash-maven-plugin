// package de.itemis.maven.plugins.unleash.actions;
//
// import java.io.File;
// import java.io.FileInputStream;
// import java.io.InputStreamReader;
//
// import org.apache.maven.model.Model;
// import org.apache.maven.model.Parent;
// import org.apache.maven.model.io.DefaultModelWriter;
// import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
// import org.apache.maven.project.MavenProject;
//
// import de.itemis.maven.plugins.unleash.util.PomUtil;
//
// public class PreparePomVersionsAction extends AbstractProcessingAction {
// private String releaseVersion;
// private String developmentVersion;
//
// public PreparePomVersionsAction(String releaseVersion, String developmentVersion) {
// this.releaseVersion = releaseVersion;
// this.developmentVersion = developmentVersion;
// }
//
// @Override
// public void execute() {
// for (MavenProject project : getReactorProjects()) {
// Model projectModel = project.getModel();
// updateVersion(projectModel);
// updateParentVersion(projectModel);
//
// try {
// // loads a plain model from the project file since project.getModel() would return a pom with mixed-in parent
// // declarations and would thus not be suitable for writing back
// MavenXpp3Reader reader = new MavenXpp3Reader();
// Model model = reader.read(new InputStreamReader(new FileInputStream(project.getFile())));
// if (model.getVersion() != null) {
// model.setVersion(projectModel.getVersion());
// }
// if (model.getParent() != null) {
// model.getParent().setVersion(projectModel.getParent().getVersion());
// }
//
// // TODO maybe do not just serialize the model but adapt the versions by hand to preserve the exact formatting of
// // the pom
// new DefaultModelWriter().write(new File(project.getFile().getAbsolutePath().concat("_new")), null, model);
// } catch (Exception e) {
// // FIXME error handling!
// // throw new MojoExecutionException(e.getMessage(), e);
// }
// }
// }
//
// // TODO outsource into another action (modifyAction)
// private void updateVersion(Model model) {
// if (model.getVersion() == null) {
// // in this case it is not necessary to update the version since the version is inherited from the parent
// return;
// }
//
// if (this.releaseVersion != null) {
// model.setVersion(this.releaseVersion);
// } else {
// // TODO handle cases where version does not end on -SNAPSHOT
// String currentVersion = model.getVersion();
// if (currentVersion.endsWith(PomUtil.VERSION_QUALIFIER_SNAPSHOT)) {
// String newVersion = currentVersion.substring(0,
// currentVersion.length() - PomUtil.VERSION_QUALIFIER_SNAPSHOT.length());
// model.setVersion(newVersion);
// }
// }
// }
//
// private void updateParentVersion(Model model) {
// Parent parent = model.getParent();
// if (parent == null) {
// return;
// }
//
// for (MavenProject project : getReactorProjects()) {
// if (project.getArtifactId().equals(parent.getArtifactId()) && project.getGroupId().equals(parent.getGroupId())) {
// parent.setVersion(project.getVersion());
// break;
// }
// }
// }
//
// @Override
// protected void doPrepare() {
// // TODO Auto-generated method stub
//
// }
//
// @Override
// protected void doValidate() {
// // TODO Auto-generated method stub
//
// }
//
// @Override
// public void rollback() {
// // TODO Auto-generated method stub
//
// }
// }
