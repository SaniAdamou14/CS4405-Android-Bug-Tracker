package com.example.bugtracker

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bugtracker.data.local.BugTrackerDatabase
import com.example.bugtracker.data.local.IssueEntity
import com.example.bugtracker.data.local.IssuePriority
import com.example.bugtracker.data.local.IssueStatus
import com.example.bugtracker.data.local.PendingOperation
import com.example.bugtracker.data.local.SyncState
import com.example.bugtracker.databinding.ActivityMainBinding
import java.util.UUID
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IssueListViewModel(
    private val database: BugTrackerDatabase
) : ViewModel() {

    private val dao = database.issueDao()

    val issues = dao.observeActiveIssues()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun addSampleIssue() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val issue = IssueEntity(
                id = UUID.randomUUID().toString(),
                title = "Sample Bug",
                description = "This is a sample bug report for testing Room persistence.",
                priority = IssuePriority.HIGH,
                status = IssueStatus.OPEN,
                createdAt = now,
                updatedAt = now,
                syncState = SyncState.PENDING,
                pendingOperation = PendingOperation.CREATE
            )
            dao.upsert(issue)
        }
    }

    fun deleteIssue(issueId: String) {
        viewModelScope.launch {
            dao.markForDeletion(issueId, System.currentTimeMillis())
        }
    }
}

class IssueListViewModelFactory(
    private val database: BugTrackerDatabase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return IssueListViewModel(database) as T
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val app by lazy { application as BugTrackerApplication }

    private val viewModel: IssueListViewModel by viewModels {
        IssueListViewModelFactory(app.database)
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
