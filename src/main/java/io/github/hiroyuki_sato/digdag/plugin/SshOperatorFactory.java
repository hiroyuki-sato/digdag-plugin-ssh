package io.github.hiroyuki_sato.digdag.plugin;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import io.digdag.client.config.Config;
import io.digdag.client.config.ConfigException;
import io.digdag.spi.Operator;
import io.digdag.spi.OperatorContext;
import io.digdag.spi.OperatorFactory;
import io.digdag.spi.SecretProvider;
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
import net.schmizz.sshj.userauth.keyprovider.BaseFileKeyProvider;
import net.schmizz.sshj.userauth.keyprovider.KeyProviderUtil;
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile;
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
            int port = params.get("port", int.class, 22);
            String user = params.get("user", String.class, System.getProperty("user.name"));

            try {
                setupKnownKey();

                logger.info(String.format("connecting %s@%s:%d", user, host, port));
                ssh.connect(host, port);

                try {

                    authorize();
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
                catch (ConnectionException ex) {
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

        private void authorize()
        {
            Config params = request.getConfig().mergeDefault(
                    request.getConfig().getNestedOrGetEmpty("ssh"));

            String user = params.get("user", String.class);
            SecretProvider secret = context.getSecrets().getSecrets("ssh");

            try {
                if (params.get("password_auth", Boolean.class, false)) {
                    Optional<String> password = secret.getSecretOptional("password");
                    if (!password.isPresent()) {
                        throw new RuntimeException("password not set");
                    }
                    ssh.authPassword(user, password.get());
                }
                else {
                    Optional<String> publicKey = secret.getSecretOptional("public_key");
                    Optional<String> privateKey = secret.getSecretOptional("private_key");
                    Optional<String> publicKeyPass = secret.getSecretOptional("public_key_passphrase");
                    if (!publicKey.isPresent()) {
                        throw new RuntimeException("public_key not set");
                    }
                    if (publicKeyPass.isPresent()) {
                        // TODO
                        // ssh.authPublickey(user,publicKey.get());
                        throw new ConfigException("public_key_passphrase doesn't support yet");
                    }
                    if(!privateKey.isPresent()){
                        throw new ConfigException("private key not set");
                    }

                    OpenSSHKeyFile keyfile = new OpenSSHKeyFile();

                    keyfile.init(privateKey.get(),publicKey.get());
                    ssh.authPublickey(user,keyfile);
                }
            }
            catch (UserAuthException | TransportException ex) {
                throw Throwables.propagate(ex);
            }
        }

        private void setupKnownKey()
        {
            try {
                ssh.addHostKeyVerifier(new PromiscuousVerifier());
                ssh.loadKnownHosts();
            }
            catch (IOException ex) {
                throw Throwables.propagate(ex);
            }
        }


    }
}
