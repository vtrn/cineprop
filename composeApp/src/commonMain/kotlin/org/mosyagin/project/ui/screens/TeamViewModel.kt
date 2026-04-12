package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mosyagin.project.ProjectMember
import org.mosyagin.project.repository.MemberRepository

class TeamViewModel(
    private val projectId: String,
    private val memberRepository: MemberRepository
) : ScreenModel {

    private val _selectedMemberId = MutableStateFlow<String?>(null)
    val selectedMemberId: StateFlow<String?> = _selectedMemberId.asStateFlow()

    private val _roleFilter = MutableStateFlow<String?>(null)
    val roleFilter: StateFlow<String?> = _roleFilter.asStateFlow()

    val members: StateFlow<List<ProjectMember>> = memberRepository.getMembersByProject(projectId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredMembers: StateFlow<List<ProjectMember>> = combine(members, _roleFilter) { list, filter ->
        if (filter == null) list else list.filter { it.role == filter }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val roles: StateFlow<List<String>> = members.map { list ->
        list.map { it.role }.distinct().sorted()
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectMember(id: String?) {
        _selectedMemberId.value = id
    }

    fun setRoleFilter(role: String?) {
        _roleFilter.value = role
    }

    fun addMember(email: String, role: String) {
        screenModelScope.launch {
            memberRepository.addMember(projectId, email, role)
        }
    }

    fun removeMember(memberId: String) {
        screenModelScope.launch {
            memberRepository.removeMember(memberId)
            if (_selectedMemberId.value == memberId) {
                _selectedMemberId.value = null
            }
        }
    }
}
