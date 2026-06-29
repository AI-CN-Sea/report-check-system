import request from './request'

export function getDashboardStats() {
  return request.get('/statistics/dashboard')
}

export function getUsers() {
  return request.get('/users')
}

export function getRoles() {
  return request.get('/users/roles')
}

export function createUser(data) {
  return request.post('/users', data)
}

export function updateUser(id, data) {
  return request.put(`/users/${id}`, data)
}

export function resetUserPassword(id, password) {
  return request.put(`/users/${id}/password`, { password })
}

export function deleteUser(id) {
  return request.delete(`/users/${id}`)
}

export function getCourses() {
  return request.get('/courses')
}

export function createCourse(data) {
  return request.post('/courses', data)
}

export function updateCourse(id, data) {
  return request.put(`/courses/${id}`, data)
}

export function deleteCourse(id) {
  return request.delete(`/courses/${id}`)
}

export function getClasses() {
  return request.get('/classes')
}

export function createClassInfo(data) {
  return request.post('/classes', data)
}

export function updateClassInfo(id, data) {
  return request.put(`/classes/${id}`, data)
}

export function deleteClassInfo(id) {
  return request.delete(`/classes/${id}`)
}

export function getStudentClasses(classId) {
  return request.get('/student-classes', {
    params: classId ? { classId } : {}
  })
}

export function addStudentClass(data) {
  return request.post('/student-classes', data)
}

export function deleteStudentClass(id) {
  return request.delete(`/student-classes/${id}`)
}

export function getExperimentTasks() {
  return request.get('/experiment-tasks')
}

export function createExperimentTask(data) {
  return request.post('/experiment-tasks', data)
}

export function updateExperimentTask(id, data) {
  return request.put(`/experiment-tasks/${id}`, data)
}

export function deleteExperimentTask(id) {
  return request.delete(`/experiment-tasks/${id}`)
}

export function getReports(taskId) {
  return request.get('/reports', {
    params: taskId ? { taskId } : {}
  })
}

export function getReportDetail(id) {
  return request.get(`/reports/${id}`)
}

export function uploadReport(data) {
  return request.post('/reports/upload', data, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function batchUploadReports(data) {
  return request.post('/reports/batch-upload', data, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function deleteReport(id) {
  return request.delete(`/reports/${id}`)
}

export function getCheckTasks() {
  return request.get('/check-tasks')
}

export function createCheckTask(data) {
  return request.post('/check-tasks', data)
}

export function getCheckResults(checkTaskId) {
  return request.get('/check-results', {
    params: checkTaskId ? { checkTaskId } : {}
  })
}

export function getCheckResultDetail(id) {
  return request.get(`/check-results/${id}`)
}

export function exportCheckResults(checkTaskId) {
  return request.get('/check-results/export', {
    params: checkTaskId ? { checkTaskId } : {},
    responseType: 'blob'
  })
}
