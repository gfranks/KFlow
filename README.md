# KFlow

Kotlin Flow Based Reactive Architecture.

# Install

### Gradle/JitPack

- Add JitPack to your top-level build.gradle file
```
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```
- Add KFlow to your module's build.gradle file
```
dependencies {
     implementation "com.github.gfranks:KFlow:0.0.2"`
}
```

# Usage

### Android View Model

Start by extending either `KFlowViewModel` (a.k.a `ViewModel`) or `KFlowAndroidViewModel` (a.k.a `AndroidViewModel`). You'll need to provide an implementation for abstract functions `emitter(): Emitter` and `bind(flow:): Flow`. Here's an example of a basic view model with only a single action, in this case `HelloWorldAction.Load`:
```kotlin
sealed class HelloWorldState {
    data class Initial(val helloWorld: HelloWorld? = null): HelloWorldState()
    object Loading: HelloWorldState()
    data class LoadSuccess(val page: Int, val nextPage: Int?, val value: HelloWorld?): HelloWorldState()
    data class LoadFailed(val error: String? = null): HelloWorldState()
}

sealed class HelloWorldAction {
    data class Load(val user: User, val page: Int = 1): HelloWorldAction()
}

private const val LinkKey = "Link"

@FlowPreview
class HelloWorldViewModel(application: Application): KFlowAndroidViewModel<HelloWorldAction, Response<HelloWorld>, HelloWorldState>(application) {

    @Inject
    lateinit var repository: HelloWorldRepository

    override fun emitter() = object : Emitter<HelloWorldAction, Response<HelloWorld>, HelloWorldState> {
        override val initialState: HelloWorldState
            get() = HelloWorldState.Initial()

        override suspend fun perform(action: HelloWorldAction): Flow<Output<HelloWorldAction, Response<HelloWorld>>> =
                flow {
                    when (action) {
                        is HelloWorldAction.Load -> emit(Output(action, repository.loadHelloWorlds(action.user, action.page)))
                    }
                }

        override suspend fun emit(action: HelloWorldAction, state: HelloWorldState, data: Response<HelloWorld>?) =
                when(action) {
                    is HelloWorldAction.Load -> {
                        data?.let {
                            val linkAttributes = it.headers().get(LinkKey)?.parseLinkNextAttributes()
                            val nextPage = linkAttributes?.get("page")
                            HelloWorldState.LoadSuccess(action.page, nextPage, it.body())
                        } ?: HelloWorldState.LoadFailed(null)
                    }
                }
    }

    init {
        HelloWorldDISingleton.getComponent(getApplication()).inject(this)
    }

    override fun bind(flow: Flow<HelloWorldState>): Flow<HelloWorldState> =
            flow
                .onStart { emit(HelloWorldState.Loading) }
                .catch { emit(HelloWorldState.LoadFailed(error = it.localizedMessage)) }

}
```

And this is the corresponding `Fragment`:
```kotlin
class HelloWorldFragment : Fragment(), OnMapReadyCallback {
    private lateinit var viewModel: HelloWorldViewModel
    private var map: GoogleMap? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(HelloWorldViewModel::class.java)
        // `viewModel.state` is a [LiveData] object with the type constraint you configure in the view model.
        // In this case, the type constraint is [HelloWorldState] (a.k.a LiveData<HelloWorldState>).
        // Any emission from this [LiveData] object will call `render` with the emitted state.
        viewModel.state.observe(viewLifecycleOwner, ::render)

        // Call `dispatch` and pass in the action type you configured in the view model.
        // In this case, that's `HelloWorldAction`.
        viewModel.dispatch(HelloWorldAction.Load(user))

        //...
    }

    /**
     * Receives all state emissions produced from the view model that pass through the emitter.
     */
    private fun render(state: HelloWorldState) {
        when(state) {
            is HelloWorldState.Initial -> Unit
            is HelloWorldState.Loading -> renderLoadingUI()
            is HelloWorldState.LoadSuccess -> {
                updateUi(state)
                state.nextPage?.let {
                    viewModel.dispatch(HelloWorldAction.Load(user, it))
                }
            }
            is HelloWorldState.LoadFailed -> renderLoadFailedUI(state.error)
        }
    }

    //...
}
```