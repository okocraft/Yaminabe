# Language resources

Yaminabe keeps translations with the module that owns the message.

- Shared translations live in `common/src/main/languages`.
- Paper-specific translations live in `paper/src/main/languages`.
- Velocity-specific translations live in `velocity/src/main/languages` when needed.

Platform builds merge the shared and platform-specific files into a single `languages/<locale>.properties` resource. A language key must be owned by only one module; duplicate keys between common and a platform fail the build.

English defaults continue to come from `DefaultMessageDefiner` definitions in code. The source language files are used for non-default bundled translations such as Japanese.
