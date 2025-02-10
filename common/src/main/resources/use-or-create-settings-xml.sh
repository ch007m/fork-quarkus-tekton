#!/usr/bin/env bash

#
# Checks if a settings.xml is provided and if not, creates one
#

[[ -f $(workspaces.maven-settings.path)/settings.xml ]] && \
  echo "using existing $(workspaces.maven-settings.path)/settings.xml" && exit 0

cat > "$(workspaces.maven-settings.path)/settings.xml" <<EOF
<settings>
<servers>
<!-- The servers added here are generated from environment variables. Don't change. -->
<!-- ### SERVER's USER INFO from ENV ### -->
</servers>
<mirrors>
<!-- The mirrors added here are generated from environment variables. Don't change. -->
<!-- ### mirrors from ENV ### -->
</mirrors>
<proxies>
<!-- The proxies added here are generated from environment variables. Don't change. -->
<!-- ### HTTP proxy from ENV ### -->
</proxies>
</settings>
EOF

xml=""
if [ -n "$(params.PROXY_HOST)" ] && [ -n "$(params.PROXY_PORT)" ]; then
  xml="<proxy>\
    <id>genproxy</id>\
    <active>true</active>\
    <protocol>$(params.PROXY_PROTOCOL)</protocol>\
    <host>$(params.PROXY_HOST)</host>\
    <port>$(params.PROXY_PORT)</port>"
      if [ -n "$(params.PROXY_USER)" ] && [ -n "$(params.PROXY_PASSWORD)" ]; then
        xml="$xml\
          <username>$(params.PROXY_USER)</username>\
          <password>$(params.PROXY_PASSWORD)</password>"
      fi
      if [ -n "$(params.PROXY_NON_PROXY_HOSTS)" ]; then
        xml="$xml\
          <nonProxyHosts>$(params.PROXY_NON_PROXY_HOSTS)</nonProxyHosts>"
      fi
      xml="$xml\
        </proxy>"
              sed -i "s|<!-- ### HTTP proxy from ENV ### -->|$xml|" "$(workspaces.maven-settings.path)/settings.xml"
fi

if [ -n "$(params.SERVER_USER)" ] && [ -n "$(params.SERVER_PASSWORD)" ]; then
  xml="<server>\
    <id>serverid</id>"
      xml="$xml\
        <username>$(params.SERVER_USER)</username>\
        <password>$(params.SERVER_PASSWORD)</password>"
              xml="$xml\
                </server>"
                              sed -i "s|<!-- ### SERVER's USER INFO from ENV ### -->|$xml|" "$(workspaces.maven-settings.path)/settings.xml"
fi

if [ -n "$(params.MAVEN_MIRROR_URL)" ]; then
  xml="    <mirror>\
    <id>mirror.default</id>\
    <url>$(params.MAVEN_MIRROR_URL)</url>\
    <mirrorOf>central</mirrorOf>\
    </mirror>"
      sed -i "s|<!-- ### mirrors from ENV ### -->|$xml|" "$(workspaces.maven-settings.path)/settings.xml"
fi

