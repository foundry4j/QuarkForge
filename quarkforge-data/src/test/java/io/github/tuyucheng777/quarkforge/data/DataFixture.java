package io.github.tuyucheng777.quarkforge.data;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/// 测试数据操作，统一在事务中执行。
@ApplicationScoped
public class DataFixture {

    @Inject
    SoftNoteRepository softNoteRepository;

    @Transactional
    public Long createTenantNote(String title) {
        TenantNote note = new TenantNote();
        note.title = title;
        note.persist();
        return note.id;
    }

    @Transactional
    public List<String> tenantNoteTitles() {
        return TenantNote.<TenantNote>findAll().list().stream().map(note -> note.title).toList();
    }

    @Transactional
    public TenantNote findTenantNote(Long id) {
        return TenantNote.findById(id);
    }

    @Transactional
    public String tenantIdOf(Long id) {
        TenantNote note = TenantNote.findById(id);
        return note == null ? null : note.tenantId;
    }

    @Transactional
    public Long createSoftNote(String title) {
        SoftNote note = new SoftNote();
        note.title = title;
        note.persist();
        return note.id;
    }

    @Transactional
    public List<String> activeSoftNoteTitles() {
        return softNoteRepository.findAll().list().stream().map(note -> note.title).toList();
    }

    @Transactional
    public List<String> allSoftNoteTitles() {
        return softNoteRepository.findAllIncludingDeleted().list().stream().map(note -> note.title).toList();
    }

    @Transactional
    public boolean deleteSoftNote(Long id) {
        return softNoteRepository.deleteById(id);
    }

    @Transactional
    public boolean restoreSoftNote(Long id) {
        return softNoteRepository.restoreById(id);
    }

    @Transactional
    public SoftNote findSoftNoteIncludingDeleted(Long id) {
        return softNoteRepository.findByIdIncludingDeleted(id);
    }

    @Transactional
    public void clearAll() {
        TenantNote.deleteAll();
        SoftNote.deleteAll();
    }
}
