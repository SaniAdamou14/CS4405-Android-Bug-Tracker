package com.example.bugtracker

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bugtracker.data.local.IssuePriority
import com.example.bugtracker.data.local.SyncState
import com.example.bugtracker.data.repository.IssueRepository
import com.example.bugtracker.databinding.ActivityMainBinding
import com.example.bugtracker.sync.SyncScheduler
import com.example.bugtracker.ui.editor.IssueEditorViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IssueListViewModel(
    private val repository: IssueRepository
) : ViewModel() {

    val issues = repository.issues
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun addSampleIssue() {
        viewModelScope.launch {
            repository.createIssue(
                title = "Sample Bug",
                description = "This is a sample bug report for testing Room persistence.",
                priority = IssuePriority.HIGH
            )
        }
    }

    fun deleteIssue(issueId: String) {
        viewModelScope.launch {
            repository.deleteIssue(issueId)
        }
    }
}

class IssueListViewModelFactory(
    private val repository: IssueRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return IssueListViewModel(repository) as T
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val app by lazy { application as BugTrackerApplication }

    private val viewModel: IssueListViewModel by viewModels {
        IssueListViewModelFactory(app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fabAdd.setOnClickListener {
            viewModel.addSampleIssue()
        }

        binding.btnSync.setOnClickListener {
            val pending = viewModel.issues.value.count {
                it.syncState != SyncState.SYNCED
            }
            binding.tvStatus.text = "Pending sync: $pending issues"
        }
    }
}
