rootProject.name = "Covid19"

// Test
include("sharedTest")

// Domain
include("domain")

// Data
include("data")
include("data:local", "data:remote")

// Presentation
include("commandLine")
include("android", "android:classic")


enableFeaturePreview("GRADLE_METADATA")
