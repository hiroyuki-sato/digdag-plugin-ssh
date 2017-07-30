package io.digdag.plugin.ssh;

import com.google.common.base.Throwables;
import io.digdag.client.config.Config;
import io.digdag.spi.Operator;
import io.digdag.spi.OperatorContext;
import io.digdag.spi.OperatorFactory;
import io.digdag.spi.TaskResult;
import io.digdag.spi.TemplateEngine;
import io.digdag.util.BaseOperator;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;

import java.io.IOException;

public class SshOperatorFactory implements OperatorFactory {
    @SuppressWarnings("unused")
    private final TemplateEngine templateEngine;

    public SshOperatorFactory(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String getType() {
        return "ssh";
    }

    final SSHClient ssh = new SSHClient();
    @Override
    public Operator newOperator(OperatorContext context) {
        return new SshOperator(context);
    }

    private class SshOperator extends BaseOperator {
        public SshOperator(OperatorContext context) {
            super(context);
        }

        @Override
        public TaskResult runTask() {
            Config params = request.getConfig().mergeDefault(
                request.getConfig().getNestedOrGetEmpty("ssh"));

            String command = params.get("_command",String.class);
            String host = params.get("host",String.class);

            try {
                ssh.addHostKeyVerifier("50:be:e8:2d:cd:1b:65:38:c0:f7:59:9f:53:e4:54:bd");
                ssh.loadKnownHosts();
                ssh.addHostKeyVerifier(new PromiscuousVerifier());
                ssh.connect(host);
            } catch (IOException ex) {
                throw Throwables.propagate(ex);
            }

            try {
                ssh.authPublickey(System.getProperty("user.name"));
                final Session session = ssh.startSession();
                final Session.Command result = session.exec(command);

            }
            catch (UserAuthException ex) {
                throw Throwables.propagate(ex);
            }
            catch (ConnectionException ex) {
                throw Throwables.propagate(ex);
            }
            catch (TransportException ex) {
                ex.printStackTrace();
            }

            return TaskResult.empty(request);
        }
    }

}
