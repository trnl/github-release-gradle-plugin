package org.gradle.api.plugins.github
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

import java.security.KeyStore

class DraftReleaseTask extends DefaultTask {
    public static final String GROUP_RELEASE = 'Github Release'

    DraftReleaseTask() {
        group = GROUP_RELEASE
    }

    @TaskAction
    void run() {
        project.github.with {

            if (!(project.git.remote ==~ 'git@github\\.com:(.+)\\/(.+)\\.git')) {
                throw new GradleException("Github repo should match 'git@github.com:{user}/{repo}.git' pattern")
            }

            def path = project.git.remote.replaceAll('git@github\\.com:(.+)\\/(.+)\\.git', {
                m -> "/repos/${m[1]}/${m[2]}/releases"
            })

            def keyStore = KeyStore.getInstance(KeyStore.defaultType)
            new File("/Users/jlee/.keystore").withInputStream {
                keyStore.load(it, "changeit".toCharArray())
            }

            def http = new HTTPBuilder("https://api.github.com${path}")
            def response = http.request(Method.POST, ContentType.JSON) {
                headers['Accept'] = 'application/vnd.github.manifold-preview'
                headers['User-Agent'] = 'Mozilla/5.0 Ubuntu/8.10 Firefox/3.0.4'
                headers['Authorization'] = 'Basic ' + "$credentials.username:$credentials.password"
                        .getBytes('iso-8859-1')
                        .encodeBase64()
                body = [
                        tag_name: release.tag.toString(),
                        name    : release.name.toString(),
                        body    : project.release.githubRelease.releaseNotes.toString(),
                        draft   : true
                ]
            }

            def releaseId = response.id

/*
            project.subprojects { subproject ->
                subproject.configurations.archives {
                    def jar = it.getAllArtifacts().find { artifact ->
                        artifact.getFile().getName().endsWith("-${project.version}.jar")
                    }

                    path = project.git.remote.replaceAll('git@github\\.com:(.+)\\/(.+)\\.git', { 
                        m -> "/repos/${m[1]}/${m[2]}/releases/${releaseId}/assets?name=${jar.getFile().getName()}"
                    })

                    http = new HTTPBuilder("https://uploads.github.com${path}")
                    def strategy = new TrustStrategy() {
                        @Override
                        boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
                            true
                        }
                    }
                    http.client.connectionManager.schemeRegistry.register(new Scheme("https", new SSLSocketFactory("TLS", keyStore,
                        "changeit", keyStore, new SecureRandom(), strategy, new X509HostnameVerifier() { 
                                @Override
                                void verify(final String host, final SSLSocket ssl) throws IOException {
                                }

                                @Override
                                void verify(final String host, final X509Certificate cert) throws SSLException {
                                }

                                @Override
                                void verify(final String host, final String[] cns, final String[] subjectAlts) throws SSLException {
                                }

                                @Override
                                boolean verify(final String s, final SSLSession sslSession) {
                                    true
                                }
                            }), 443) )
                    println "Uploading ${jar.getFile()}"
                    def uploadResponse = http.request(Method.POST, ContentType.BINARY) {
                        headers['Accept'] = 'application/vnd.github.manifold-preview'
                        headers['Origin'] = 'github.com'
                        headers['User-Agent'] = 'Mozilla/5.0 Ubuntu/8.10 Firefox/3.0.4'
                        headers['Authorization'] = 'Basic ' + "$credentials.username:$credentials.password"
                                .getBytes('iso-8859-1')
                                .encodeBase64()

                        body = jar.getFile().getBytes();
                    }
                }
            }
*/

        }
    }

    private String prop(String name) {
        project.hasProperty(name) ? project.property(name) as String : null
    }

}
