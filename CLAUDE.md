# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Clash Meta for Android** is an open-source Android VPN client that provides a graphical UI for [Clash.Meta](https://github.com/MetaCubeX/Clash.Meta), a Golang-based VPN/proxy engine. The app is built with Kotlin, Android Architecture Components, and supports multiple CPU architectures (arm64-v8a, armeabi-v7a, x86, x86_64).

### Key Technical Stack
- **Language**: Kotlin + Java
- **Build**: Gradle with CMake for Go code compilation
- **Architecture**: Multi-module (app, common, core, design, service, hideapi)
- **Android SDK**: Target API 35, Min API 21
- **UI**: DataBinding + Material 3 Design System with custom Design<Request> pattern
- **Networking**: OkHttp for HTTP requests
- **Async**: Coroutines with Channels for event-driven architecture
- **Persistence**: SharedPreferences with Store pattern delegates
- **Go Integration**: Embedded Clash.Meta via JNI (CMake build)

## Build & Development Commands

### Prerequisites
```bash
# Update git submodules (required - includes Clash.Meta kernel)
git submodule update --init --recursive

# Environment setup
# 1. OpenJDK 21 (not 11 as README states - check build.gradle.kts)
# 2. Android SDK (API 35)
# 3. Golang 1.25+
# 4. CMake for native builds
```

### Configure for local builds
Create `local.properties` in project root:
```properties
sdk.dir=/path/to/android-sdk

# Optional: Custom app package name (default: com.github.metacubex.clash)
custom.application.id=com.my.package.clash
remove.suffix=true

# Optional: API endpoint for V2Board authentication
api.base.url=https://your-v2board-api.com/api/v1
```

### Build Commands

| Task | Command | Output |
|------|---------|--------|
| **Debug build** | `./gradlew app:assembleAlphaDebug` | Alpha debug APK (unsigned) |
| **Release build** (unsigned) | `./gradlew app:assembleAlphaRelease` | Alpha release APK (unsigned) |
| **Production build** | `./gradlew app:assembleMetaRelease` | Meta release APK (requires signing.properties) |
| **Clean** | `./gradlew clean` | Full rebuild, clears cache |
| **Build all modules** | `./gradlew build` | Compiles all submodules |

### Gradle Properties (gradle.properties)
- `org.gradle.jvmargs=-Xmx4g -XX:+UseZGC`: JVM memory tuning (4GB, GC optimized)
- `org.gradle.parallel=true`: Parallel build enabled
- `kotlin.code.style=official`: Kotlin formatting standard

### APK Output Locations
- **Debug**: `app/build/outputs/apk/alpha/debug/`
- **Alpha Release**: `app/build/outputs/apk/alpha/release/`
- **Meta Release**: `app/build/outputs/apk/meta/release/`

**Note**: This project requires Android SDK to be installed. Building fails at configuration time without it.

## Project Architecture

### Module Structure (Gradle subprojects)

| Module | Purpose | Key Classes |
|--------|---------|-------------|
| **app** | Main Android app entry point | `MainActivity`, `BaseActivity`, all Activity implementations |
| **design** | UI layer with DataBinding layouts | `Design<R>` base class, all Design implementations, RecyclerView adapters |
| **service** | Business logic & networking | V2Board API, AuthStore, ProfileManager, ClashService |
| **common** | Shared utilities & extensions | Store pattern, Components.kt (intent helpers), compat layers |
| **core** | JNI bridge to Go runtime | ClashBridge, Profile models, Proxy models |
| **hideapi** | Reflection utilities for hidden Android APIs | Used for accessing internal Android functionality |

### Core Architectural Pattern: Design<Request>

This project uses a unified event-driven architecture across all Activities:

```
┌─────────────────────────────────────────────────────────────┐
│ Activity (BaseActivity<D>)                                  │
├─────────────────────────────────────────────────────────────┤
│ - Extends BaseActivity<DesignType>                         │
│ - Implements suspend fun main() with event loop            │
│ - Receives UI events via design.requests Channel           │
│ - Emits app events via events Channel                      │
└─────────────────────────────────────────────────────────────┘
        ↓ receives                          ↑ sends
        │ layout inflate                    │ state updates
        │ & binding                         │
        ↓                                   ↑
┌─────────────────────────────────────────────────────────────┐
│ Design<Request> (in design module)                          │
├─────────────────────────────────────────────────────────────┤
│ - abstract class Design<R>(context: Context)               │
│ - val root: View (DataBinding inflated)                    │
│ - val requests: Channel<R> (UI event stream)               │
│ - sealed class Request (UI action enum)                    │
│ - suspend fun showToast() for user feedback                │
└─────────────────────────────────────────────────────────────┘
        ↓ inflates                         ↑ binds data
        │ XML layout                       │ via DataBinding
        │                                  │
        ↓                                  ↑
┌─────────────────────────────────────────────────────────────┐
│ XML Layout (design/res/layout/)                             │
├─────────────────────────────────────────────────────────────┤
│ - DataBinding enabled (binding class auto-generated)        │
│ - Variables: self (Design), data models                     │
│ - onClick listeners: requests.trySend(Request.Action)      │
└─────────────────────────────────────────────────────────────┘
```

#### Event Loop Pattern (All Activities)

```kotlin
class ExampleActivity : BaseActivity<ExampleDesign>() {
    override suspend fun main() {
        val design = ExampleDesign(this)
        setContentDesign(design)

        while (isActive) {
            select<Unit> {
                // System events (service updates, permissions, etc.)
                events.onReceive {
                    when (it) {
                        Event.ServiceRecreated -> design.refresh()
                        // ...
                    }
                }

                // UI events (button clicks, form submissions)
                design.requests.onReceive {
                    when (it) {
                        is ExampleDesign.Request.Action -> {
                            // Call repository/service methods
                            // Update design state
                            // Launch other activities via startActivity()
                        }
                    }
                }
            }
        }
    }
}
```

**Key Points**:
- `while (isActive)` runs until activity is destroyed
- `select<Unit>` waits for events from multiple channels
- Coroutine-based: no callbacks, no LiveData
- Activities are lifecycled with their coroutines

### Module Dependency Flow

```
app/ ─────────────┬─→ design/ ──┬─→ common/
                  │             │
                  └─→ service/ ─┼─→ common/
                      (V2Board) │
                                └─→ core/ ──→ hideapi/
```

- **app** depends on: design, service, common, core
- **design** depends on: common, core (for models)
- **service** depends on: common, core
- **common** depends on: (no inter-project deps)
- **core** depends on: hideapi (optional, for internal Android APIs)

### Key Architectural Decisions

#### 1. Design<Request> with Channels (not ViewModel/LiveData)
- **Why**: Explicit event flow, type-safe requests, works with coroutine lifecycle
- **Pattern**: Sealed class Request for compile-time exhaustiveness
- **Usage**: `design.requests.trySend(Request.Action(data))` from XML `android:onClick`

#### 2. Store Pattern for Persistence (SharedPreferences delegates)
```kotlin
class AuthStore(context: Context) {
    private val store = Store(context.getSharedPreferences(...).asStoreProvider())
    var token: String by store.string("token", defaultValue = "")
    var isLoggedIn: Boolean get() = token.isNotBlank()
}
```
- **Benefits**: Type-safe, automatic serialization, single source of truth
- **Location**: Each store in its module (design/store/, service/store/)

#### 3. V2Board API Layer (Kotlin Serialization + OkHttp)
```
V2BoardApi (HTTP client)
    ↓ parses response envelope { data: {...}, message, code }
V2BoardRepository (API coordinator + AuthStore)
    ↓ exposes high-level methods
Activities (call repository methods)
```
- **Error Handling**: Sealed class `ApiResult<T>` with 5 subtypes
  - `Success(data)`
  - `ValidationError(errors: Map<String, List<String>>)`
  - `AuthError(message)` - triggers redirect to LoginActivity
  - `NetworkError`
  - `ServerError(message)`

#### 4. Profile Import Flow (Subscription URL)
```kotlin
// In LoginActivity after successful login
withProfile {
    val uuid = create(Profile.Type.Url, "V2Board", subscribeUrl)
    commit(uuid)
    setActive(queryByUUID(uuid)!!)
}
```
- Profiles go through 3 states: Pending → Processing → Imported
- `commit()` triggers ProfileProcessor.apply() (runs validation, updates)
- `setActive()` marks as current proxy profile

#### 5. BuildConfig Injection (API_BASE_URL)
- **Source 1**: Environment variable `API_BASE_URL`
- **Source 2**: local.properties key `api.base.url`
- **Default**: `https://example.com/api/v1` (placeholder)
- **Usage**: `BuildConfig.API_BASE_URL` in code
- **CI/CD**: GitHub Actions passes via secrets

## Code Structure Patterns

### Creating a New Activity + Design UI

1. **Create Design class** (in design module):
```kotlin
// design/src/main/java/.../ExampleDesign.kt
class ExampleDesign(context: Context) : Design<ExampleDesign.Request>(context) {
    sealed class Request {
        data class Submit(val data: String) : Request()
        object Cancel : Request()
    }

    private val binding = DesignExampleBinding.inflate(context.layoutInflater, context.root, false)
    override val root: View get() = binding.root

    suspend fun setLoading(loading: Boolean) {
        withContext(Dispatchers.Main) {
            binding.loading = loading
        }
    }
}
```

2. **Create XML Layout** (with DataBinding):
```xml
<!-- design/res/layout/design_example.xml -->
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android">
    <data>
        <variable name="self" type="com.github.kr328.clash.design.ExampleDesign" />
        <variable name="loading" type="Boolean" />
    </data>
    <CoordinatorLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">
        <!-- UI elements -->
        <Button
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Submit"
            android:onClick="@{() -> self.requests.trySend(self.new ExampleDesign.Request.Submit())}" />
    </CoordinatorLayout>
</layout>
```
**Key**: Binding class auto-generated as `DesignExampleBinding` from filename `design_example.xml`

3. **Create Activity** (in app module):
```kotlin
// app/src/main/java/.../ExampleActivity.kt
class ExampleActivity : BaseActivity<ExampleDesign>() {
    private val repository by lazy { ExampleRepository(this) }

    override suspend fun main() {
        val design = ExampleDesign(this)
        setContentDesign(design)

        while (isActive) {
            select<Unit> {
                events.onReceive { }
                design.requests.onReceive {
                    when (it) {
                        is ExampleDesign.Request.Submit -> {
                            design.setLoading(true)
                            when (val result = repository.submit(it.data)) {
                                is ApiResult.Success -> finish()
                                else -> design.showError("Failed")
                            }
                            design.setLoading(false)
                        }
                    }
                }
            }
        }
    }
}
```

4. **Register in Manifest** (app/src/main/AndroidManifest.xml):
```xml
<activity android:name=".ExampleActivity" ... />
```

### DataBinding Best Practices

- **Naming**: XML filename → binding class name (snake_case → PascalCase)
  - `design_login.xml` → `DesignLoginBinding`
- **Inflation**: Use `LayoutInflater.inflate()` via binding class
  - `binding = DesignLoginBinding.inflate(context.layoutInflater, null, false)`
  - `root = binding.root`
- **Data binding**: Update via `binding.field = value` on Main dispatcher
- **Click handlers**: `android:onClick="@{() -> self.requests.trySend(Request)}"` in XML

### Intent Helpers (from common/util/Components.kt)

```kotlin
// Instead of Intent(context, MyActivity::class.java)
val intent = MyActivity::class.intent

// Instead of ComponentName("package", "class.name")
val componentName = MyActivity::class.componentName
```

### Store Pattern (SharedPreferences delegates)

```kotlin
class AuthStore(context: Context) {
    private val store = Store(
        context.getSharedPreferences("auth", Context.MODE_PRIVATE).asStoreProvider()
    )

    // String property with default
    var token: String by store.string("token", defaultValue = "")

    // Boolean property
    var isLoggedIn: Boolean by store.boolean("logged_in", defaultValue = false)

    // Enum property
    var mode: AuthMode by store.enum("mode", defaultValue = AuthMode.Local, AuthMode.values())

    // Computed property (no storage)
    fun clear() {
        token = ""
        isLoggedIn = false
    }
}
```

### Kotlin Serialization (for API models)

```kotlin
@Serializable
data class UserInfo(
    val id: Int,
    val email: String,
    @SerialName("plan_id")
    val planId: Int? = null,
    @SerialName("transfer_enable")
    val transferEnable: Long = 0
)
```
- Use `@Serializable` on data classes
- Use `@SerialName("key")` to map JSON keys
- Default values for optional fields

## V2Board Integration Details

Located in `service/src/main/java/com/github/kr328/clash/service/v2board/`:

### V2BoardApi.kt
- OkHttp client with 10-second timeout
- Automatic `Authorization` header injection (Bearer token)
- JSON parsing with `kotlinx.serialization`
- V2Board response envelope parsing: `{ data: {...}, message, code }`
- Error handling: 422 validation, 401 auth, 500+ server errors
- All network calls run on `Dispatchers.IO`

### V2BoardRepository.kt
- High-level API methods: `login()`, `register()`, `logout()`, `getSubscribeUrl()`, etc.
- Stores auth token + user email in `AuthStore`
- Returns sealed class `ApiResult<T>` for type-safe error handling

### V2BoardModels.kt
- `@Serializable` data classes for all endpoints
- Key models: `UserInfo`, `Plan`, `ServerNode`, `Order`, `PaymentMethod`

### Integration in MainActivity
- Auth gate at startup: redirect to `LoginActivity` if not logged in
- Auto-import subscription: fetch URL and create profile via `withProfile {}`
- Added menu items: Dashboard, User Info, Logout

## GitHub Actions & CI/CD

### Workflows (in .github/workflows/)

| Workflow | Trigger | Outputs |
|----------|---------|---------|
| **build-debug.yaml** | Push to main / PR | 5 debug APKs (one per architecture) |
| **build-pre-release.yaml** | Push to main | Tagged pre-release with APKs |
| **build-release.yaml** | Manual (workflow_dispatch) | Tagged release with signed APKs |
| **update-dependencies.yaml** | Upstream Clash.Meta update | Auto-PR with dependency updates |

### Build Environment
- **Java**: Temurin JDK 21
- **Gradle**: gradle/actions/setup-gradle
- **Go**: 1.25+ (check-latest: true)
- **Cache**: Go build cache & module cache

### Environment Variables (set in GitHub Secrets)
- `API_BASE_URL`: V2Board API endpoint (e.g., `https://your-domain.com/api/v1`)
- `SIGNING_STORE_PASSWORD`: Keystore password (for release builds)
- `SIGNING_KEY_ALIAS`: Key alias in keystore
- `SIGNING_KEY_PASSWORD`: Key password

### Release Process
1. **Pre-release**: Manually trigger `build-pre-release.yaml` → creates `Prerelease-alpha` tag
2. **Release**: Manually trigger `build-release.yaml` with tag `v1.2.3` → auto-bumps version, creates release
   - Version is stored in `build.gradle.kts` (versionName, versionCode)

## Common Development Tasks

### Add a new screen (Activity + Design)
1. Create Design class in `design/src/main/java/.../design/`
2. Create XML layout in `design/res/layout/design_*.xml` with DataBinding
3. Create Activity in `app/src/main/java/.../` extending `BaseActivity<YourDesign>`
4. Add Activity to `app/AndroidManifest.xml`
5. Add strings/colors to `design/res/values/strings.xml` and `colors.xml`

### Modify API endpoints
- Edit `service/.../v2board/V2BoardEndpoints.kt` (constants)
- Edit `service/.../v2board/V2BoardModels.kt` (add @Serializable data classes)
- Update `service/.../v2board/V2BoardRepository.kt` (add method)
- Update Activities to call new method and handle ApiResult

### Change app theme colors
- Edit `design/res/values/colors.xml` (light theme)
- Edit `design/res/values-night/colors.xml` (dark theme)
- All layouts reference `?attr/colorPrimary`, `?attr/colorSecondary`, etc. → updates propagate automatically

### Add persistent user preference
1. Add property to `AuthStore` or `UiStore` using Store pattern delegates
2. Use in Activity: `authStore.myProperty = value` (auto-saves to SharedPreferences)
3. Read: `val value = authStore.myProperty`

### Debug V2Board API calls
- Check `V2BoardApi.post()` and `get()` methods for request/response logging
- Verify `API_BASE_URL` in `BuildConfig` (set via env var or local.properties)
- Check `AuthStore.token` is set after login
- Verify response envelope: `{ data: {...}, message, code }`

## Testing Notes

- **No unit test framework configured** in this project (common for Android apps focused on UI)
- **Manual testing**: Build APK, install on device/emulator, verify features
- **Emulator requirements**: Arm64 or x86_64, API 21+
- **Device testing preferred**: Real devices have actual VPN/proxy functionality

## Important Notes

### Submodules
- **Clash.Meta kernel** is a git submodule in `core/src/foss/golang/`
- Must run `git submodule update --init --recursive` after clone
- Kernel updates trigger automatic GitHub Actions PR

### Profile Processing
- Profiles imported via `withProfile { create(...); commit(...) }` go through validation
- `ProfileProcessor.apply()` is called during `commit()`
- Profiles must be marked active with `setActive()` to be used

### Android Permissions
- Internet access: `<uses-permission android:name="android.permission.INTERNET" />`
- VPN access: `<uses-permission android:name="android.permission.BIND_VPN_SERVICE" />`
- etc. (declared in AndroidManifest.xml)

### Code Style
- Follow Android Studio/IntelliJ IDEA project code style (File → Settings → Editor → Code Style)
- Use official Kotlin style guide (configured in gradle.properties)

## References

- [Android Architecture Components](https://developer.android.com/topic/libraries/architecture)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Kotlin Serialization](https://github.com/Kotlin/kotlinx.serialization)
- [Material Design 3](https://m3.material.io/)
- [Clash.Meta Documentation](https://github.com/MetaCubeX/Clash.Meta)
