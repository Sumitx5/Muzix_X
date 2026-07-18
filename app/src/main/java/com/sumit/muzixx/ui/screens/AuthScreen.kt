package com.sumit.muzixx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    val currentUser = authViewModel.currentUser
    val authState by authViewModel.authState.collectAsState(initial = AuthState.Idle)
    var emailInput by remember { mutableStateOf(currentUser?.email ?: "") }
    var passwordInput by remember { mutableStateOf("") }
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
            onAuthSuccess()
            authViewModel.resetState()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = "Manage Account", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier.padding(bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (currentUser?.photoUrl != null) {
                    AsyncImage(
                        model = currentUser.photoUrl,
                        contentDescription = "Profile Pic",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }
            }

            if (authState is AuthState.Error) {
                Text(
                    text = (authState as AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(shape = RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        enabled = currentUser == null,
                        leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )

                    if (currentUser == null) {
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        )
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
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
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            genderOptions.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        genderInput = selectionOption
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
                Button(
                    onClick = {
                        val finalName = nameInput.trim()
                        viewModel.settings.updateUserName(finalName)
                        viewModel.settings.updateUserGender(genderInput)

                        if (currentUser != null) {
                            authViewModel.updateProfileData(newName = finalName, newGender = genderInput)
                        }
                        onAuthSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Text("Save Profile Changes", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                if (currentUser == null) {
                    Button(
                        onClick = {
                            val email = emailInput.trim()
                            val password = passwordInput.trim()

                            if (email.isNotEmpty() && password.isNotEmpty()) {
                                authViewModel.authenticateWithEmailPassword(
                                    email = email,
                                    password = password,
                                    defaultDisplayName = nameInput.trim(),
                                    defaultGender = genderInput,
                                    localSongsHeard = viewModel.stats.totalSongsHeardState.intValue,
                                    localMonthlySongs = viewModel.stats.monthlySongsHeardState.intValue,
                                    localYearlySongs = viewModel.stats.yearlySongsHeardState.intValue,
                                    localTotalSeconds = viewModel.stats.totalPlaySecondsState.longValue,
                                    localMonthlySeconds = viewModel.stats.monthlyPlaySecondsState.longValue,
                                    localYearlySeconds = viewModel.stats.yearlyPlaySecondsState.longValue
                                )
                            }
                        },
                        enabled = authState !is AuthState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = "Connect Account",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    TextButton(
                        onClick = {
                            authViewModel.logout()
                            viewModel.resetCloudSyncFlag()
                            onBackClick()
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(text = "Logout from Cloud Account", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}