package com.sumit.muzixx.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sumit.muzixx.viewmodel.AuthState
import com.sumit.muzixx.viewmodel.AuthViewModel
import com.sumit.muzixx.viewmodel.MusicViewModel
import com.sumit.muzixx.utils.glassEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    viewModel: MusicViewModel,
    onAuthSuccess: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val currentUser = authViewModel.currentUser
    val authState by authViewModel.authState.collectAsState(initial = AuthState.Idle)

    var emailInput by remember { mutableStateOf(currentUser?.email ?: "") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var nameInput by remember {
        mutableStateOf(currentUser?.displayName ?: viewModel.settings.userName)
    }
    var genderInput by remember {
        mutableStateOf(viewModel.settings.userGender)
    }

    var dropdownExpanded by remember { mutableStateOf(false) }
    val genderOptions = listOf("Male", "Female", "Prefer Not to Say")

    val accentColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            emailInput = currentUser.email ?: ""
            nameInput = currentUser.displayName ?: viewModel.settings.userName
            genderInput = viewModel.settings.userGender
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            Toast.makeText(context, "Account connected successfully!", Toast.LENGTH_SHORT).show()
            onAuthSuccess()
            authViewModel.resetState()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (currentUser != null) "Manage Account" else "Connect Account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (currentUser?.photoUrl != null) {
                    AsyncImage(
                        model = currentUser.photoUrl,
                        contentDescription = "Profile Pic",
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Icon",
                            tint = accentColor,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }
            }

            if (authState is AuthState.Error) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(shape = RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        enabled = currentUser == null && authState !is AuthState.Loading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = if (currentUser == null) ImeAction.Next else ImeAction.Done
                        ),
                        leadingIcon = {
                            Icon(Icons.Default.Mail, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (currentUser == null) {
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password") },
                            singleLine = true,
                            enabled = authState !is AuthState.Loading,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = genderInput,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender") },
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            genderOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        genderInput = option
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentUser == null) {
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            val email = emailInput.trim()
                            val password = passwordInput.trim()

                            if (email.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Please enter both email and password.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            authViewModel.authenticateWithEmailPassword(
                                email = email,
                                password = password,
                                defaultDisplayName = nameInput.trim().ifBlank { "User" },
                                defaultGender = genderInput,
                                localSongsHeard = viewModel.stats.totalSongsHeardState.intValue,
                                localMonthlySongs = viewModel.stats.monthlySongsHeardState.intValue,
                                localYearlySongs = viewModel.stats.yearlySongsHeardState.intValue,
                                localTotalSeconds = viewModel.stats.totalPlaySecondsState.longValue,
                                localMonthlySeconds = viewModel.stats.monthlyPlaySecondsState.longValue,
                                localYearlySeconds = viewModel.stats.yearlyPlaySecondsState.longValue
                            )
                        },
                        enabled = authState !is AuthState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "Connect Account",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val finalName = nameInput.trim().ifBlank { "User" }
                            viewModel.settings.updateUserName(finalName)
                            viewModel.settings.updateUserGender(genderInput)
                            Toast.makeText(context, "Preferences saved locally.", Toast.LENGTH_SHORT).show()
                            onBackClick()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Save Offline Preferences Only", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = {
                            val finalName = nameInput.trim().ifBlank { "User" }
                            viewModel.settings.updateUserName(finalName)
                            viewModel.settings.updateUserGender(genderInput)
                            authViewModel.updateProfileData(newName = finalName, newGender = genderInput)
                            Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                            onBackClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Save Profile Changes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    TextButton(
                        onClick = {
                            authViewModel.logout()
                            viewModel.resetCloudSyncFlag()
                            Toast.makeText(context, "Logged out.", Toast.LENGTH_SHORT).show()
                            onBackClick()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Logout from Cloud Account",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}