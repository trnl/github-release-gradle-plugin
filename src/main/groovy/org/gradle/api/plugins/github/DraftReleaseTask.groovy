package org.gradle.api.plugins.github

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.conn.ssl.TrustStrategy
import org.apache.http.conn.ssl.X509HostnameVerifier
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

import javax.net.ssl.SSLException
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocket
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

class DraftReleaseTask extends DefaultTask {
    public static final String GROUP_RELEASE = 'Github Release'

    DraftReleaseTask() {
        group = GROUP_RELEASE
    }

    @TaskAction
    void run() {

        if (!(project.git.remote ==~ 'git@github\\.com:(.+)\\/(.+)\\.git')) {
            throw new GradleException("Github repo should match 'git@github.com:{user}/{repo}.git' pattern")
        }

        project.github.with {
            def releaseId = createRelease()

            uploadArchives(releaseId)

        }
    }

    def void uploadArchives(releaseId) {
        def keyStore = KeyStore.getInstance(KeyStore.defaultType)
        new File("/Users/jlee/.keystore").withInputStream {
            keyStore.load(it, "changeit".toCharArray())
        }

        project.subprojects { subproject ->
            subproject.configurations.archives { archive ->
                uploadArchive(archive, releaseId, keyStore)
            }
        }
    }

    def createRelease() {
        def notes = project.release.githubRelease.releaseNotes
        if (notes == null) {
            notes = defaultNotes()
        }

        project.github.with {
            def path = project.git.remote.replaceAll('git@github\\.com:(.+)\\/(.+)\\.git', {
                m -> "/repos/${m[1]}/${m[2]}/releases"
            })

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
                        body    : notes.toString(),
                        draft   : true
                ]
            }

            response.id
        }
    }

    def void uploadArchive(archive, releaseId, keyStore) {
        project.github.with {
            def jar = archive.getAllArtifacts().find { artifact ->
                artifact.getFile().getName().endsWith("-${project.version}.jar")
            }

            def path = project.git.remote.replaceAll('git@github\\.com:(.+)\\/(.+)\\.git', {
                m -> "/repos/${m[1]}/${m[2]}/releases/${releaseId}/assets?name=${jar.getFile().getName()}"
            })

            def http = new HTTPBuilder("https://uploads.github.com${path}")
            def strategy = new TrustStrategy() {
                @Override
                boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
                    true
                }
            }
            http.client.connectionManager.schemeRegistry.register(
                    new Scheme("https", new SSLSocketFactory("TLS", keyStore, "changeit", keyStore, new SecureRandom(), strategy, new X509HostnameVerifier() {
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
                    }), 443))
            println "Uploading ${jar.getFile()}"
            http.request(Method.POST, ContentType.BINARY) {
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

    def defaultNotes() {
        def mileStone = getMilestone(project.release.version) 
        "Hey!  I'm releasing $project.release.version today!"
    }

    def getMilestone(version) {
        
    }
}
