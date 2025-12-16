package com.example.dealtracker.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.dealtracker.data.remote.api.UserUpdateRequest
import com.example.dealtracker.domain.UserManager
import com.example.dealtracker.domain.model.User
import com.example.dealtracker.ui.theme.AppTheme
import kotlinx.coroutines.launch
import com.example.dealtracker.data.remote.repository.RetrofitClient

// Edit user profile screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavHostController) {
    val colors = AppTheme.colors
    val fontScale = AppTheme.fontScale
    val currentUser by UserManager.currentUser.collectAsState()
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(currentUser?.name ?: "") }
    var gender by remember { mutableStateOf(currentUser?.gender ?: "Male") }
    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = (20 * fontScale).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.topBarBackground,
                    titleContentColor = colors.topBarContent,
                    navigationIconContentColor = colors.topBarContent
                )
            )
        },
        containerColor = colors.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                ProfileTextField(
                    label = "Name",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Enter your name",
                    enabled = !isLoading
                )

                Column {
                    Text(
                        text = "Gender",
                        fontSize = (14 * fontScale).sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.primaryText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { if (!isLoading) expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = gender,
                            onValueChange = {},
                            readOnly = true,
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.border,
                                disabledBorderColor = colors.border,
                                focusedContainerColor = colors.surface,
                                unfocusedContainerColor = colors.surface,
                                disabledContainerColor = colors.card
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            genderOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            option,
                                            fontSize = (16 * fontScale).sp
                                        )
                                    },
                                    onClick = {
                                        gender = option
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                ProfileTextField(
                    label = "Email",
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Enter your email",
                    enabled = !isLoading
                )

                ProfilePasswordField(
                    label = "New Password",
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Leave empty to keep current password",
                    enabled = !isLoading
                )

                if (password.isNotEmpty()) {
                    ProfilePasswordField(
                        label = "Confirm Password",
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = "Re-enter your password",
                        enabled = !isLoading
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (password.isNotEmpty() && password != confirmPassword) {
                            errorMessage = "Passwords do not match"
                            showErrorDialog = true
                            return@Button
                        }

                        isLoading = true
                        scope.launch {
                            try {
                                val uid = currentUser?.uid ?: 1

                                val request = UserUpdateRequest(
                                    name = name,
                                    email = email,
                                    gender = gender,
                                    password = if (password.isNotEmpty()) password else null
                                )

                                val response = RetrofitClient.userApi.updateUser(uid, request)

                                isLoading = false

                                if (response.isSuccessful && response.body()?.success == true) {
                                    val updatedUser = response.body()?.user
                                    if (updatedUser != null) {
                                        UserManager.setUser(
                                            User(
                                                uid = updatedUser.uid,
                                                name = updatedUser.name,
                                                email = updatedUser.email,
                                                gender = updatedUser.gender
                                            )
                                        )
                                    }
                                    showSuccessDialog = true
                                } else {
                                    val errorBody = response.errorBody()?.string()
                                    errorMessage = response.body()?.error
                                        ?: errorBody
                                                ?: "Failed to update profile. Please try again."
                                    showErrorDialog = true
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = "Network error: ${e.message}"
                                showErrorDialog = true
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        disabledContainerColor = colors.border
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = colors.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Save Changes",
                            fontSize = (16 * fontScale).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.primaryText.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.accent)
                }
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                navController.popBackStack()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = colors.success,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Success",
                    fontWeight = FontWeight.Bold,
                    fontSize = (18 * fontScale).sp
                )
            },
            text = {
                Text(
                    "Your profile has been updated successfully!",
                    fontSize = (14 * fontScale).sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        navController.popBackStack()
                    }
                ) {
                    Text(
                        "OK",
                        color = colors.accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = (14 * fontScale).sp
                    )
                }
            }
        )
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = {
                Text(
                    text = "Error",
                    fontWeight = FontWeight.Bold,
                    color = colors.error,
                    fontSize = (18 * fontScale).sp
                )
            },
            text = {
                Text(
                    errorMessage,
                    fontSize = (14 * fontScale).sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showErrorDialog = false }
                ) {
                    Text(
                        "OK",
                        color = colors.accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = (14 * fontScale).sp
                    )
                }
            }
        )
    }
}

// Text field component
@Composable
fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean = true
) {
    val colors = AppTheme.colors
    val fontScale = AppTheme.fontScale

    Column {
        Text(
            text = label,
            fontSize = (14 * fontScale).sp,
            fontWeight = FontWeight.Medium,
            color = colors.primaryText,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            placeholder = {
                Text(
                    placeholder,
                    color = colors.tertiaryText,
                    fontSize = (14 * fontScale).sp
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.border,
                disabledBorderColor = colors.border,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                disabledContainerColor = colors.card,
                cursorColor = colors.accent
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

// Password field component
@Composable
fun ProfilePasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean = true
) {
    val colors = AppTheme.colors
    val fontScale = AppTheme.fontScale

    Column {
        Text(
            text = label,
            fontSize = (14 * fontScale).sp,
            fontWeight = FontWeight.Medium,
            color = colors.primaryText,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            placeholder = {
                Text(
                    placeholder,
                    color = colors.tertiaryText,
                    fontSize = (14 * fontScale).sp
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.border,
                disabledBorderColor = colors.border,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                disabledContainerColor = colors.card,
                cursorColor = colors.accent
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}