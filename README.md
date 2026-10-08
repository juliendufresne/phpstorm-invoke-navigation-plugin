# PHP __invoke Navigation

A PhpStorm plugin: Cmd/Ctrl+Click (Go to Declaration) on the parentheses of an invokable-object call jumps to the
class's `__invoke()` method.

```php
($this->handler)($args);    // click on any of the parentheses
$callback($args);           // click on ( or )
$this->getHandler()($args); // click on the last ( or )
```

Clicking `handler`, `$callback` and so on keeps the default behavior (go to the property or variable). Union types
offer every matching `__invoke()`, and inherited `__invoke()` methods are found.

## Build

The plugin zip is plain JVM bytecode, so a zip built on one OS installs on any OS. Build wherever is convenient.

Gradle needs a JDK 25. PhpStorm's bundled JBR works, or use any other JDK 25 install. Pass `-PphpstormPath` to
compile against a local PhpStorm, or drop it to download PhpStorm 2026.2 instead.

### macOS

```sh
export JAVA_HOME=~/Applications/PhpStorm.app/Contents/jbr/Contents/Home
./gradlew buildPlugin -PphpstormPath=$HOME/Applications/PhpStorm.app
```

### Linux

`phpstormPath` is the PhpStorm install directory (the one containing `bin/` and `product-info.json`). Common
locations:

- Toolbox: `~/.local/share/JetBrains/Toolbox/apps/phpstorm`
- Tarball: wherever it was extracted, for example `/opt/PhpStorm-262.xxxx`
- Snap: `/snap/phpstorm/current`

```sh
PHPSTORM=~/.local/share/JetBrains/Toolbox/apps/phpstorm
export JAVA_HOME=$PHPSTORM/jbr
./gradlew buildPlugin -PphpstormPath=$PHPSTORM
```

### Output

The plugin zip is written to `build/distributions/`. Install it with Settings > Plugins > gear icon > Install Plugin
from Disk.

Other tasks: `./gradlew test` runs the tests, `./gradlew runIde` starts a sandboxed PhpStorm with the plugin loaded.
