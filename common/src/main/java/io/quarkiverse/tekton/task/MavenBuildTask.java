package io.quarkiverse.tekton.task;

import io.fabric8.tekton.v1.Task;
import io.fabric8.tekton.v1.TaskBuilder;
import io.quarkiverse.tekton.common.utils.Resources;

public class MavenBuildTask {

    private static final String USE_OR_CREATE_SETTTINGS_XML = Resources.read("/use-or-create-settings-xml.sh");

    public static Task create() {
        return new TaskBuilder()
                .withApiVersion("tekton.dev/v1")
                .withKind("Task")
                .withNewMetadata()
                .withName("maven")
                .addToAnnotations("tekton.dev/pipelines.minVersion", "0.17.0")
                .addToAnnotations("tekton.dev/categories", "Build Tools")
                .addToAnnotations("tekton.dev/tags", "build-tool")
                .addToAnnotations("tekton.dev/platforms", "linux/amd64,linux/s390x,linux/ppc64le")
                .endMetadata()
                .withNewSpec()
                .withDescription("This Task can be used to run a Maven build. It uses a workspace to store m2 local repo.")
                // -------------------------------
                // Workspaces
                // -------------------------------
                .addNewWorkspace()
                .withName("project-dir")
                .withDescription("The workspace consisting of maven project.")
                .endWorkspace()
                .addNewWorkspace()
                .withName("maven-settings")
                .withDescription("The workspace consisting of the custom maven settings provided by the user.")
                .endWorkspace()
                .addNewWorkspace()
                .withName("maven-m2-repo")
                .withDescription("Local repo (m2) workspace")
                .withOptional(true)
                .endWorkspace()
                .addNewWorkspace()
                .withName("dockerconfig")
                .withDescription("The workspace containing the docker config.")
                .withOptional(true)
                .endWorkspace()
                // -------------------------------
                // Params
                // -------------------------------
                .addNewParam()
                .withName("MAVEN_IMAGE")
                .withDescription("Maven base image")
                .withType("string")
                .withNewDefault("ghcr.io/carlossg/maven:3.9.6-eclipse-temurin-17")
                .endParam()
                .addNewParam()
                .withName("GOALS")
                .withDescription("maven goals to run")
                .withType("array")
                .withNewDefault()
                .withArrayVal("package")
                .endDefault()
                .endParam()

                .addNewParam()
                .withName("MAVEN_MIRROR_URL")
                .withDescription("The Maven repository mirror url")
                .withType("string")
                .withNewDefault("")
                .endParam()
                .addNewParam()
                .withName("SERVER_USER")
                .withDescription("The username for the server")
                .withType("string")
                .withNewDefault("")
                .endParam()
                .addNewParam()
                .withName("SERVER_PASSWORD")
                .withDescription("The password for the server")
                .withType("string")
                .withNewDefault("")
                .endParam()
                .addNewParam()
                .withName("PROXY_USER")
                .withDescription("The username for the proxy server")
                .withType("string")
                .withNewDefault("")
                .endParam()
                .addNewParam()
                .withName("PROXY_PASSWORD")
                .withDescription("The password for the proxy server")
                .withType("string")
                .withNewDefault("")
                .endParam()
                .addNewParam()
                .withName("PROXY_PORT")
                .withDescription("Port number for the proxy server")
                .withType("string")
                .withNewDefault("")
                .endParam()
                .addNewParam()
                .withName("PROXY_HOST")
                .withDescription("Proxy server Host")
                .withType("string")
                .withNewDefault("")
                .endParam()
                .addNewParam()
                .withName("PROXY_NON_PROXY_HOSTS")
                .withDescription("Non proxy server host")
                .withType("string")
                .withNewDefault("")
                .endParam()
                .addNewParam()
                .withName("PROXY_PROTOCOL")
                .withDescription("Protocol for the proxy ie http or https")
                .withType("string")
                .withNewDefault("http")
                .endParam()
                .addNewParam()
                .withName("CONTEXT_DIR")
                .withDescription(
                        "The context directory within the repository for sources on which we want to execute maven goals.")
                .withType("string")
                .withNewDefault(".")
                .endParam()
                .addNewParam()
                .withName("DOCKER_HOST")
                .withDescription("The docker host (e.g. tcp://hostvm.mynamspace:2376).")
                .withType("string")
                .withNewDefault("tcp://localhost:2376")
                .endParam()
                .addNewParam()
                .withName("DOCKER_CONFIG")
                .withDescription("Path to the docker.json file containing auths")
                .withType("string")
                .withNewDefault("")
                .endParam()
                .addNewParam()
                .withName("TESTCONTAINERS_RYUK_DISABLED")
                .withDescription("Disable the ryuk container ... (true/false)")
                .withType("string")
                .withNewDefault("false")
                .endParam()
                .addNewParam()
                .withName("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE")
                .withDescription("Override the docker socket to use for the testcontainers library.")
                .withType("string")
                .withNewDefault("/run/user/1000/podman/podman.sock")
                .endParam()
                // -------------------------------
                // Steps
                // -------------------------------
                // Step 1: mvn-settings
                .addNewStep()
                .withName("mvn-settings")
                .withImage("registry.access.redhat.com/ubi8/ubi-minimal:8.2")
                .withScript(USE_OR_CREATE_SETTTINGS_XML)
                .endStep()
                // Step 2: mvn-goals
                .addNewStep()
                .withName("mvn-goals")
                .withImage("$(params.MAVEN_IMAGE)")
                .withWorkingDir("$(workspaces.project-dir.path)/$(params.CONTEXT_DIR)")
                .addNewEnv()
                .withName("DOCKER_CONFIG")
                .withValue("$(params.DOCKER_CONFIG)")
                .endEnv()
                .addNewEnv()
                .withName("DOCKER_HOST")
                .withValue("$(params.DOCKER_HOST)")
                .endEnv()
                .addNewEnv()
                // Hard-coded to "true" in the YAML step
                .withName("TESTCONTAINERS_RYUK_DISABLED")
                .withValue("true")
                .endEnv()
                .withCommand("/usr/bin/mvn")
                .withArgs(
                        "-s",
                        "$(workspaces.maven-settings.path)/settings.xml",
                        "$(params.GOALS)",
                        "-Dmaven.repo.local=$(workspaces.maven-m2-repo.path)/.m2")
                .endStep()
                .endSpec()
                .build();
    }
}
