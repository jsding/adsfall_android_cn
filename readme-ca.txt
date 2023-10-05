推送存储库：

export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain playstrap --domain-owner 777969164926 --query authorizationToken --output text`

publishing {
  publications {
      mavenJava(MavenPublication) {
          groupId = '<groupId>'
          artifactId = '<artifactId>'
          version = '<version>'
          from components.java
      }
  }
  repositories {
      maven {
          url 'https://playstrap-777969164926.d.codeartifact.ap-southeast-1.amazonaws.com/maven/adsfall/'
          credentials {
              username "aws"
              password System.env.CODEARTIFACT_AUTH_TOKEN
          }
      }
  }
}

从存储库获取
export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain playstrap --domain-owner 777969164926 --query authorizationToken --output text`
maven {
  url 'https://playstrap-777969164926.d.codeartifact.ap-southeast-1.amazonaws.com/maven/adsfall/'
  credentials {
      username "aws"
      password System.env.CODEARTIFACT_AUTH_TOKEN
  }
}