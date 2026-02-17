# Arsen
### Professional GUI binary disassembler with multi-format support, multi-architecture analysis, and much more.
###### (i originally started this project for fun, but now im actively trying to maintain it)

## Supported Formats
- PE (Windows)
- ELF (Linux)
- MachO (macOS)

## Supported Architectures
- x86
- x86-64
- ARM
- ARM64
- MIPS (Detection only!!)
- PowerPC (Detection only!!)

## Plugin Development

Creating a Plugin
```java
public class MyAnalyzer implements Plugin {
    @Override
    public String getName() {
        return "My Custom Analyzer";
    }

    @Override
    public void initialize(PluginContext context) {
        //plugin init
    }

    @Override
    public void shutdown() {
        //cleanup
    }
}
```

## Contributions
Contributions are always welcome!
