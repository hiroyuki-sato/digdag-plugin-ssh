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
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SshOperatorFactory
        implements OperatorFactory
{
    @SuppressWarnings("unused")
    private final TemplateEngine templateEngine;

    private static Logger logger = LoggerFactory.getLogger(SshOperatorFactory.class);

    public SshOperatorFactory(TemplateEngine templateEngine)
    {
        this.templateEngine = templateEngine;
    }

    public String getType()
    {
        return "ssh";
    }

    final SSHClient ssh = new SSHClient();

    @Override
    public Operator newOperator(OperatorContext context)
    {
        return new SshOperator(context);
    }

    private class SshOperator
            extends BaseOperator
    {
        public SshOperator(OperatorContext context)
        {
            super(context);
        }

        @Override
        public TaskResult runTask()
        {
            Config params = request.getConfig().mergeDefault(
                    request.getConfig().getNestedOrGetEmpty("ssh"));

            String command = params.get("_command", String.class);
            String host = params.get("host", String.class);
            int port = params.get("port",int.class,22);
            String keyPath = params.get("key_path", String.class);
            String user = params.get("user", String.class, System.getProperty("user.name"));

            try {
                ssh.addHostKeyVerifier(new PromiscuousVerifier());
                ssh.loadKnownHosts();

                logger.info(String.format("connecting %s@%s:%d",user,host,port));
                ssh.connect(host,port);
//                ssh.authPassword("user", "xxxxx");

                try {
                    logger.info(String.format("user = %s, key_path: %s", user, keyPath));
                    ssh.authPublickey(user, keyPath);
                    final Session session = ssh.startSession();

                    logger.info("Execute: " + command);
                    final Session.Command result = session.exec(command);
                    result.join(10, TimeUnit.SECONDS);

                    System.out.println("result" + result);
                    int status = result.getExitStatus();
                    logger.info("Result: " + IOUtils.readFully(result.getInputStream()).toString());
                    logger.info("Status: " + status);
                    if (status != 0) {
                        throw new RuntimeException("Command failed with code " + status);
                    }
                }
                catch (UserAuthException | TransportException | ConnectionException ex) {
                    ex.printStackTrace();
                    throw Throwables.propagate(ex);
                }
                finally {
                    ssh.close();
                }
                ssh.disconnect();
            }
            catch (IOException ex) {
                throw Throwables.propagate(ex);
            }

            return TaskResult.empty(request);
        }
    }
}
