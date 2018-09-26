# digdag-plugin-ssh

Digdag `ssh>` operator plugin to execute a remote command via ssh.

This plugin supports two type of authentications. One is public key
authentication(default). The other is password authentication.

## Configuration

[Release list](https://github.com/hiroyuki-sato/digdag-plugin-ssh/releases).

### Public key authentication

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

Local mode

```
digdag secrets --local --set ssh.private_key=@id_rsa
digdag secrets --local --set ssh.public_key=@id_rsa.pub
```

Server mode

```
digdag secrets --project <project> --set ssh.private_key=@id_rsa
digdag secrets --project <project> --set ssh.public_key=@id_rsa.pub
```

### Password authentication

```
digdag secrets --local --set ssh.password=@alice_passwd_file
digdag secrets --local --set ssh.bob_passwd=@bob_passwd_file
```

```yaml
_export:
  ssh:
    # global setting
    #host: host.add.re.ss
    #user: username
    stdout_log: true # Output stdout log (default true)
    stderr_log: true # Output stderr log (default false)
    password_auth: true #
  plugin:
    repositories:
      - https://jitpack.io
    dependencies:
      - com.github.hiroyuki-sato:digdag-plugin-ssh:0.1.4 # Modify latest version.

+step1:
  ssh>: ls
  host: remote.host.name
  port: 22
  user: alice
  # `ssh.password` value used by default.

+step1:
  ssh>: ls
  host: remote.host.name2
  port: 22
  user: bob
  password_override: bob_passwd
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

## Maintainers

* Hiroyuki Sato(@hiroyuki-sato)
* Yuki Iwamoto(@yuhiwa)
