# ML Kit finds its components at startup by reflection: MlKitInitProvider asks
# ComponentDiscovery for every ComponentRegistrar named in the merged manifest and
# instantiates each one through its no-arg constructor.
#
# firebase-components 16.1.0 — the version com.google.mlkit:genai-prompt pulls in — ships a
# consumer rule that keeps those classes but not their members:
#
#     -keep class * implements com.google.firebase.components.ComponentRegistrar
#
# R8's full mode (the default since AGP 8) does not imply the default constructor the way
# compat mode did, so the constructor is shrunk away, discovery dies on
# `NoSuchMethodException: CommonComponentRegistrar.<init> []`, and every later checkStatus()
# reports Gemini Nano as unavailable — with no crash and no exception for GeminiNanoClient to
# catch, so the translation feature simply disappears from Settings in release builds.
#
# firebase-components 19.0.0 fixed its own rule to the form below; this restates it here
# instead of forcing an upgrade of a transitive ML Kit dependency.
-keep class * implements com.google.firebase.components.ComponentRegistrar { void <init>(); }
