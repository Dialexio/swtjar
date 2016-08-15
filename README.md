Full details available on the [SWTJar Site](http://mchr3k.github.com/swtjar/)

----

I made a small edit to allow adding multiple architectures for Linux. I don't imagine that an SWT JAR for x86-based Linux will work on PowerPC.

SWT filenames should be named as such:
* Linux: `swt-linux-{ARCH}-{SWT VERSION}.jar`
* Mac: `swt-macOS-x86_64-{SWT VERSION}.jar`
* Windows: `swt-win-{ARCH}-{SWT VERSION}.jar`

For the architecture, `i386` and `x86` are treated as synonyms. In addition, `x86-64`, `x86_64`, and `amd64` are also synonyms for each other.