# digdag-plugin-ssh

Digdag `ssh>` operator plugin to execute a remote command via ssh.

## configuration

[Release list](https://github.com/hiroyuki-sato/digdag-plugin-ssh/releases).

```yaml
_export:
  ssh:
    host: host.add.re.ss
    user: username
    stdout_log: true # Output stdout log (default true)
    stderr_log: true # Output stderr log (default false)
  plugin:
    repositories:
      - https://jitpack.io
    dependencies:
      - com.github.hiroyuki-sato:digdag-plugin-ssh:0.1.4 # Modify latest version.

+step1:
  ssh>: ls
#  host: remote.host.name
#  port: 22
#  user: hiroysato
```

Register private and public key into secrets.

local mode 

```
digdag secrets --local --set ssh.private_key=@id_rsa
digdag secrets --local --set ssh.public_key=@id_rsa.pub
```

server mode 

```
digdag secrets --project <project> --set ssh.private_key=@id_rsa
digdag secrets --project <project> --set ssh.public_key=@id_rsa.pub
```


## Development

### 1) build

```sh
./gradlew publish
```

Artifacts are build on local repos: `./build/repo`.

### 2) run an example

```sh
digdag selfupdate

rm -rf sample/.digdag/plugin 
digdag run -a --project sample plugin.dig -p repos=`pwd`/build/repo
```
