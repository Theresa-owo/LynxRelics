param([Parameter(ValueFromRemainingArguments=$true)][string[]]$GradleArgs)
$ErrorActionPreference = 'Stop'
$taskJava = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } elseif (Test-Path -LiteralPath 'D:/Environment/Amazon Corretto/coretto25/bin/java.exe') { 'D:/Environment/Amazon Corretto/coretto25/bin/java.exe' } else { 'java.exe' }
& $taskJava '-classpath' "$PSScriptRoot/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain @GradleArgs
exit $LASTEXITCODE
