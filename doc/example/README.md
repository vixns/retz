# Ansible Playbook Example

You'll need `jdk-8u102-linux-x64.tar.gz` file in your local file where
you run following script, as we can't automatically download JDK:

```
$ ansible-playbook centos7.yml -i inventory --ssh-common-args="-i id_rsa" -v
```
