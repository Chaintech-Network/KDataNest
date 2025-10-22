package network.chaintech.cmpstoragemanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalFocusManager
import cmpstoragemanager.composeapp.generated.resources.Res
import cmpstoragemanager.composeapp.generated.resources.logo
import kotlinx.serialization.Serializable
import localDbStorage.DocumentDatabase
import localDbStorage.find
import localDbStorage.getAllDocuments
import localDbStorage.getSchemaVersion
import localDbStorage.migrateCollection
import localDbStorage.migrateIfNeeded
import localDbStorage.query
import localDbStorage.save
import network.chaintech.sqldelight.data.local.database.Database
import network.chaintech.sqldelight.data.local.database.DatabaseQueries
import network.chaintech.storagemanager.cachestorage.CacheStorageFactory
import network.chaintech.storagemanager.keychainstorage.KeychainStorageFactory
import network.chaintech.storagemanager.localpreference.PreferenceFactory
import network.chaintech.storagemanager.sql.createDriver
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Duration.Companion.minutes


@Serializable
data class UserOld(val id: String, val name: String, val phone: Long, val books: List<Book>)
@Serializable
data class User(val id: String, val name: String, val phone: Long, val books: List<Book>,val age: Int = 0)

@Serializable
data class Book(val id: String, val name: String, val price: Double)

@Composable
@Preview
fun App() {
    val storage = KeychainStorageFactory.create()
    var key by remember { mutableStateOf("") }
    var prefKey by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var prefVal by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var cacheStorage = CacheStorageFactory.create()
    cacheStorage.countLimit = 3
    var preferenceManager = PreferenceFactory.create()
    val driver = createDriver()
    val dbQueries: DatabaseQueries = Database(driver).databaseQueries
    val db = DocumentDatabase.create(queries = dbQueries)
    println("VERSION: schema ${db.getSchemaVersion()}")
    db.migrateIfNeeded(targetVersion = 1) { version ->
        println("VERSION: $version")
        if (version == 1) {
            db.migrateCollection<UserOld, User>("users") { old ->
                User(
                    id = old.id,
                    name = old.name,
                    age = 2,
                    books = old.books,
                    phone = old.phone
                )
            }
        }
    }

    var outputPreferences by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0C0019))
                .verticalScroll(rememberScrollState())
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
                .padding(16.dp)
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = null,
                modifier = Modifier
                    .height(125.dp)
                    .padding(bottom = 20.dp)
                    .padding(top = 20.dp)
                    .fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Storage Overview Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val storageInfo = listOf(
                    Triple("🔒 Keychain / Secure", "Sensitive & encrypted", "Ideal for passwords, tokens, or any sensitive data."),
                    Triple("💾 Preferences", "Simple key-value", "Ideal for user settings, feature flags, or small app configs."),
                    Triple("⏳ Cache", "Temporary & fast", "Ideal for network responses, computed results, or session data."),
                    Triple("📂 Document DB", "Document-oriented db for serialized data models", "Ideal for user profiles, collections, or offline structured data.")
                )
                // Assign brighter border colors to the 4 cards
                val overviewBorders = listOf(
                    BorderStroke(width = 2.dp, color = Color(0xFF00FF00)), // bright green
                    BorderStroke(width = 2.dp, color = Color(0xFFFF00FF)), // neon pink
                    BorderStroke(width = 2.dp, color = Color(0xFF00B0FF)), // bright blue
                    BorderStroke(width = 2.dp, color = Color(0xFFFFEB3B))  // yellow
                )
                storageInfo.forEachIndexed { idx, (title, subtitle, description) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                        border = overviewBorders[idx]
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(2.dp))
                            Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                        }
                    }
                }
            }

            // KeychainStorage Section
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                border = BorderStroke(width = 4.dp, Color(0xFF00FF00)), // bright green
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Keychain Storage \uD83D\uDD12",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        val features = listOf(
                            "🔒 Persistent",
                            "🛡️ AES-encrypted",
                            "📱 Device-secure"
                        )

                        features.forEach { text ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(color = Color(0xFF4CAF50), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✔", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text("Enter key") },
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(size = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Enter value") },
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(size = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val keychainButtons = listOf(
                        "💾 Save" to {
                            val success = storage.save(key, value)
                            output = "Save success: $success"
                        },
                        "🔍 Get" to {
                            val storedValue = storage.get(key)
                            output = "Got: ${storedValue ?: "null"}"
                        },
                        "✏️ Update" to {
                            val success = storage.update(key, value)
                            output = "Update success: $success"
                        },
                        "🗑️ Delete" to {
                            val success = storage.remove(key)
                            output = "Delete success: $success"
                        },
                        "🔎 Contains" to {
                            val exists = storage.contains(key)
                            output = "Contains: $exists"
                        }
                    )
                    TwoPerRowButtons(buttons = keychainButtons)

                    Spacer(modifier = Modifier.height(16.dp))


                    if (output.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(Color.Gray.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Result: $output", fontWeight = FontWeight.SemiBold)
                        }
                    }

                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Preferences Section
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                border = BorderStroke(width = 4.dp, Color(0xFFFF00FF)), // neon pink
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Preferences Storage \uD83D\uDCBE",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    val features = listOf(
                        "💾 Persistent",
                        "🔑 Key-value",
                        "⚡ Fast"
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        features.forEach { text ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(color = Color(0xFF4CAF50), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✔", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = prefKey,
                        onValueChange = { prefKey = it },
                        label = { Text("Enter key") },
                        shape = RoundedCornerShape(size = 12.dp),
                        modifier = Modifier.fillMaxSize()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = prefVal,
                        onValueChange = { prefVal = it },
                        label = { Text("Enter value") },
                        shape = RoundedCornerShape(size = 12.dp),
                        modifier = Modifier.fillMaxSize()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val prefButtons = listOf(
                        "💾 Save" to {
                            preferenceManager.put(prefKey, prefVal)
                            outputPreferences = "Save success"
                        },
                        "🔍 Get" to {
                            val storedValue = preferenceManager.get<String>(prefKey)
                            outputPreferences = "Got: ${storedValue ?: "null"}"
                        },
                        "✏️ Update" to {
                            preferenceManager.put(prefKey, prefVal)
                            outputPreferences = "Update success"
                        },
                        "🗑️ Delete" to {
                            preferenceManager.remove(prefKey)
                            outputPreferences = "Delete success"
                        },
                        "🔎 Contains" to {
                            val exists = preferenceManager.contains(prefKey)
                            outputPreferences = "Contains: $exists"
                        }
                    )
                    TwoPerRowButtons(buttons = prefButtons)

                    Spacer(modifier = Modifier.height(16.dp))

                    if (outputPreferences.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(Color.Gray.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Result: $outputPreferences", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // cacheStorage Section
            var cacheKey by remember { mutableStateOf("") }
            var cacheVal by remember { mutableStateOf("") }
            var cacheOutput by remember { mutableStateOf("") }

            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                border = BorderStroke(width = 4.dp, Color(0xFF00B0FF)), // bright blue
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Cache Storage ⏳",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    val features = listOf(
                        "⏳ Temporary",
                        "🗑️ Auto-evict",
                        "⚡ Fast"
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        features.forEach { text ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(color = Color(0xFF4CAF50), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✔", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = cacheKey,
                        onValueChange = { cacheKey = it },
                        label = { Text("Enter key") },
                        shape = RoundedCornerShape(size = 12.dp),
                        modifier = Modifier.fillMaxSize()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = cacheVal,
                        onValueChange = { cacheVal = it },
                        label = { Text("Enter value") },
                        shape = RoundedCornerShape(size = 12.dp),
                        modifier = Modifier.fillMaxSize()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val cacheButtons = listOf(
                        "💾 Save" to {
                            cacheStorage.put(cacheKey, cacheVal, ttl = 10.minutes)
                            cacheOutput = "Save success"
                        },
                        "🔍 Get" to {
                            val storedValue = cacheStorage.get<String>(cacheKey)
                            cacheOutput = "Got: ${storedValue ?: "null"}"
                        },
                        "🗑️ Delete" to {
                            val success = cacheStorage.remove(cacheKey)
                            cacheOutput = "Delete success: $success"
                        },
                        "🔎 Contains" to {
                            val exists = cacheStorage.contains(cacheKey)
                            cacheOutput = "Contains: $exists"
                        }
                    )
                    TwoPerRowButtons(buttons = cacheButtons)

                    Spacer(modifier = Modifier.height(16.dp))

                    if (cacheOutput.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(Color.Gray.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Result: $cacheOutput", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // LocalDB Section
//            Card(
//                modifier = Modifier.fillMaxSize(),
//                shape = RoundedCornerShape(12.dp),
//                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
//                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
//            ) {
//                Column(
//                    modifier = Modifier.padding(16.dp),
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    Text(
//                        text = "Local Database",
//                        style = MaterialTheme.typography.titleLarge,
//                        modifier = Modifier.padding(bottom = 4.dp)
//                    )
//                    Text(
//                        text = "Local document-based NoSQL layer",
//                        style = MaterialTheme.typography.bodyMedium,
//                        modifier = Modifier.padding(bottom = 16.dp)
//                    )
//
//                    var books: MutableList<Book> = mutableListOf()
//                    var books1: MutableList<Book> = mutableListOf()
//                    val swiftBook = Book(id = "b1", name = "Intro to Swift", price = 20.0)
//                    val kotlinBook = Book(id = "b2", name = "Intro to Kotlin", price = 15.0)
//                    books.add(swiftBook)
//                    books1.add(swiftBook)
//                    books.add(kotlinBook)
                    //            val userToStore = User(id = "1", name = "Jubal", phone = 9082645138, books = books)
                    //            val userToStore2 = User(id = "2", name = "Jacob", phone = 9072645138, books = books1)
                    //            val userToStore3 = User(id = "3", name = "Ace", phone = 9082645138, books = books)

                    // Save
                    //            db.save("users", userToStore.id, userToStore)
                    //            db.save("users", userToStore2.id, userToStore2)
                    //            db.save("users", userToStore3.id, userToStore3)
                    //            db.saveAll("users",listOf("1" to userToStore,"2" to userToStore2,"3" to userToStore3))

                    // Assume we have a User model: User(id: String, name: String, phone: String)
                    //            db.update<User>("users", userToStore3.id) { user ->
                    //                user.copy(name = "Aryan") // update only the name
                    //            }

//                    val allUsers: List<User> = db.getAllDocuments("users")
//                    Text("${allUsers.count()}")
//
//                    val user: User? = db.find("users", "1")
//
//                    val kotlinUsers = db.query<User>("users") { u ->
//                        u.books.any { it.name.contains("Kotlin") }
//                    }
//
//                    Text("Users who own a Kotlin book:")
//                    kotlinUsers.forEach { u ->
//                        Text("${u.name} - owns:")
//                    }
//
//                    Text("${user?.name} ${user?.phone} - owns: ${user?.books?.joinToString { it.name }} - age ${user?.age}")
//                }
//            }


            // Inside your App() Column, after cacheStorage Section

            // LocalDB Interactive Demo
            var userId by remember { mutableStateOf("") }
            var userName by remember { mutableStateOf("") }
            var userPhone by remember { mutableStateOf("") }
            var userAge by remember { mutableStateOf("") }
            var bookName by remember { mutableStateOf("") }
            var allUsersOutput by remember { mutableStateOf<List<User>>(emptyList()) }
            var queryOutput by remember { mutableStateOf<List<User>>(emptyList()) }

            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                border = BorderStroke(width = 4.dp, Color(0xFFFFEB3B)), // yellow
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Document DB Storage \uD83D\uDCC2",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        val features = listOf(
                            "📂 Document-oriented",
                            "💾 Persistent",
                            "🔍 Queryable",
                            "🛠️ Flexible schema"
                        )

                        features.forEach { text ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Green checkmark circle
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(color = Color(0xFF4CAF50), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "✔",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Feature text with original emoji
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }

                    // Input fields
                    OutlinedTextField(
                        value = userId,
                        onValueChange = { userId = it },
                        label = { Text("User ID") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(size = 12.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(size = 12.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = userPhone,
                        onValueChange = { userPhone = it },
                        label = { Text("Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(size = 12.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = userAge,
                        onValueChange = { userAge = it },
                        label = { Text("Age") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(size = 12.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bookName,
                        onValueChange = { bookName = it },
                        label = { Text("Book Name (comma separated for multiple)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(size = 12.dp)
                    )
                    Spacer(Modifier.height(16.dp))

                    // Action buttons
                    val documentDbButtons = listOf(
                        "➕ Add / Save" to {
                            val books = bookName.split(",").mapIndexed { index, name -> Book(id = "b$index", name = name.trim(), price = 10.0) }
                            val user = User(
                                id = userId,
                                name = userName,
                                phone = userPhone.toLongOrNull() ?: 0L,
                                age = userAge.toIntOrNull() ?: 0,
                                books = books
                            )
                            db.save("users", userId, user)
                            allUsersOutput = db.getAllDocuments("users")
                        },
                        "🔍 Find" to {
                            val user = db.find<User>("users", userId)
                            if (user != null) {
                                userName = user.name
                                userPhone = user.phone.toString()
                                userAge = user.age.toString()
                                bookName = user.books.joinToString(",") { it.name }
                            }
                        },
                        "✏️ Update" to {
                            val user = db.find<User>("users", userId)
                            if (user != null) {
                                val updatedBooks = bookName.split(",").mapIndexed { index, name -> Book(id = "b$index", name = name.trim(), price = 10.0) }
                                val updatedUser = user.copy(
                                    name = userName,
                                    phone = userPhone.toLongOrNull() ?: user.phone,
                                    age = userAge.toIntOrNull() ?: user.age,
                                    books = updatedBooks
                                )
                                db.save("users", userId, updatedUser)
                                allUsersOutput = db.getAllDocuments("users")
                            }
                        },
                        "🗑️ Delete" to {
                            db.delete("users", userId)
                            allUsersOutput = db.getAllDocuments("users")
                        },
                        "🔎 Query" to {
                            queryOutput = db.query<User>("users") { u ->
                                val nameMatches = userName.isNotBlank() && u.name.contains(userName, ignoreCase = true)
                                val bookMatches = bookName.isNotBlank() && u.books.any { it.name.contains(bookName, ignoreCase = true) }
                                nameMatches || bookMatches
                            }
                        },
                        "📋 List All" to {
                            allUsersOutput = db.getAllDocuments("users")
                        }
                    )
                    TwoPerRowButtons(buttons = documentDbButtons)

                    Spacer(Modifier.height(16.dp))

                    // Output
                    if (allUsersOutput.isNotEmpty()) {
                        Text("All Users:", style = MaterialTheme.typography.titleMedium)
                        allUsersOutput.forEach { u ->
                            Text("${u.id} | ${u.name} | ${u.phone} | Age: ${u.age} | Books: ${u.books.joinToString { it.name }}")
                        }
                    }

                    Spacer(Modifier.height(8.dp))


                    if (queryOutput.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(Color.Gray.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Text("Query Result:", style = MaterialTheme.typography.titleMedium)
                                queryOutput.forEach { u ->
                                    Text("${u.id} | ${u.name} | ${u.phone} | Age: ${u.age} | Books: ${u.books.joinToString { it.name }}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper composable to display buttons two per row, preserving styles and spacing
@Composable
fun TwoPerRowButtons(
    buttons: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier
) {
    val buttonColor = androidx.compose.material3.ButtonDefaults.buttonColors(
        containerColor = Color(0xFF383838),
        contentColor = Color.White
    )
    val shape = RoundedCornerShape(size = 14.dp)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val pairs = buttons.chunked(2)
        pairs.forEach { rowButtons ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowButtons.forEach { (text, action) ->
                    Button(
                        onClick = action,
                        shape = shape,
                        colors = buttonColor,
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text(text)
                    }
                }
                if (rowButtons.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}