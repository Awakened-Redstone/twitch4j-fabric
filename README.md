# Twitch4J as a library mod 

You can see the Twitch4J project at https://github.com/twitch4j/twitch4j  

This mod allows other mods that use the API to without having to include the API and it's dependencies in the mod jar, and include it in `fabric.mod.json` if wanted

Please only report issues related to the mod, issues with the API report [here](https://github.com/twitch4j/twitch4j/issues) \
For support with the mod ping `@Awakened Redstone` at `#help-any-topic` in the Twitch4J discord, for help with Twitch4J itself just ask in the main help channels


```gradle
repositories {
    mavenCentral()
}

dependencies {
    modImplementation "com.github.twitch4j:twitch4j-fabric:${project.twitch4j_version}"
}
```
