"use client";

import React, { useState, useEffect } from 'react';
import { supabase } from '@/lib/supabaseClient';
import Papa from 'papaparse';
import { 
  Users, BookOpen, GraduationCap, LogOut, UserPlus, 
  Trash2, Key, Plus, RefreshCw, Edit, Settings, BookPlus, 
  UploadCloud, Loader2, Download, FileSpreadsheet, ShieldCheck, BarChart3
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger, DialogFooter, DialogDescription } from '@/components/ui/dialog';
import { Checkbox } from '@/components/ui/checkbox';

// --- CONSTANTS ---
const CURRENT_SESSION = "2024/2025";
const CURRENT_TERM = "2nd Term";

// --- TYPES ---
type User = { id: string; name: string; username: string; role: string; current_class_id?: string; managed_class_id?: string; primary_subject_id?: string; additional_subject_ids?: string[] };
type Class = { id: string; name: string; base: string; level: string; subjects: string[] };
type Subject = { id: string; name: string; teacher_id?: string };
type Grade = { id: string; student_id: string; subject_id: string; score: number };

// --- UTILITY ---
const getSubjectName = (id: string, subjects: Subject[]) => subjects.find(s => s.id === id)?.name || "Unknown";

// --- COMPONENTS ---

const LoginScreen = ({ onLogin }: { onLogin: (u: User) => void }) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const { data, error } = await supabase
        .from('users')
        .select('*')
        .eq('username', username)
        .eq('password', password)
        .single();

      if (error || !data) throw new Error('Invalid credentials');
      onLogin(data);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex items-center justify-center h-screen bg-slate-50 p-4">
      <Card className="w-full max-w-md shadow-xl border-t-4 border-t-green-600">
        <CardHeader className="text-center">
          <div className="mx-auto bg-green-700 w-16 h-16 rounded-full flex items-center justify-center mb-4">
            <GraduationCap className="text-white w-8 h-8" />
          </div>
          <CardTitle className="text-2xl">Elite Schools Portal</CardTitle>
          <CardDescription>Version 15: Complete Feature Set</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleLogin} className="space-y-4">
            <div className="space-y-2"><Label>Username</Label><Input value={username} onChange={e => setUsername(e.target.value)} disabled={loading}/></div>
            <div className="space-y-2"><Label>Password</Label><Input type="password" value={password} onChange={e => setPassword(e.target.value)} disabled={loading}/></div>
            {error && <div className="text-red-500 text-sm">{error}</div>}
            <Button type="submit" className="w-full bg-green-700 hover:bg-green-800" disabled={loading}>
              {loading ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : "Sign In"}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};

const StudentDashboard = ({ user, classes, subjects }: { user: User, classes: Class[], subjects: Subject[] }) => {
  const [grades, setGrades] = useState<Grade[]>([]);
  const [loading, setLoading] = useState(true);
  const currentClass = classes.find(c => c.id === user.current_class_id);

  useEffect(() => {
    const fetchGrades = async () => {
      const { data } = await supabase.from('grades').select('*').eq('student_id', user.id).eq('session', CURRENT_SESSION).eq('term', CURRENT_TERM);
      if (data) setGrades(data);
      setLoading(false);
    };
    fetchGrades();
  }, [user.id]);

  if (loading) return <div className="p-8">Loading academic record...</div>;

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
        <Card><CardHeader className="pb-2"><CardTitle className="text-xs">Class</CardTitle></CardHeader><CardContent><div className="text-lg font-bold">{currentClass?.name || "Unassigned"}</div></CardContent></Card>
        <Card><CardHeader className="pb-2"><CardTitle className="text-xs">Term</CardTitle></CardHeader><CardContent><div className="text-lg font-bold">{CURRENT_TERM}</div></CardContent></Card>
        <Card><CardHeader className="pb-2"><CardTitle className="text-xs">Average</CardTitle></CardHeader><CardContent><div className="text-2xl font-bold text-blue-600">
          {grades.length > 0 ? (grades.reduce((a, b) => a + b.score, 0) / grades.length).toFixed(1) : 0}%
        </div></CardContent></Card>
      </div>
      <Card>
        <CardHeader><CardTitle>Report Sheet</CardTitle></CardHeader>
        <CardContent>
          <Table>
            <TableHeader><TableRow><TableHead>Subject</TableHead><TableHead>Score</TableHead><TableHead>Grade</TableHead></TableRow></TableHeader>
            <TableBody>
              {currentClass?.subjects?.map(subId => {
                const grade = grades.find(g => g.subject_id === subId);
                let letter = grade ? (grade.score >= 75 ? "A1" : grade.score >= 50 ? "C6" : "F9") : "-";
                return (
                  <TableRow key={subId}>
                    <TableCell>{getSubjectName(subId, subjects)}</TableCell>
                    <TableCell>{grade ? grade.score : "-"}</TableCell>
                    <TableCell><Badge variant="outline">{letter}</Badge></TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
};

const TeacherDashboard = ({ user, classes, subjects }: { user: User, classes: Class[], subjects: Subject[] }) => {
  const allowedSubjectIds = [user.primary_subject_id, ...(user.additional_subject_ids || [])].filter(Boolean);
  const managedClass = classes.find(c => c.id === user.managed_class_id);
  
  const [selectedSubjectId, setSelectedSubjectId] = useState(user.primary_subject_id || '');
  const [selectedClassId, setSelectedClassId] = useState('');
  const [students, setStudents] = useState<User[]>([]);
  const [grades, setGrades] = useState<Grade[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    if (selectedClassId && selectedSubjectId) fetchClassData();
  }, [selectedClassId, selectedSubjectId]);

  const fetchClassData = async () => {
    setLoading(true);
    const { data: studentData } = await supabase.from('users').select('*').eq('current_class_id', selectedClassId).eq('role', 'student');
    const { data: gradeData } = await supabase.from('grades').select('*')
      .eq('class_id', selectedClassId).eq('subject_id', selectedSubjectId)
      .eq('session', CURRENT_SESSION).eq('term', CURRENT_TERM);
    setStudents(studentData || []);
    setGrades(gradeData || []);
    setLoading(false);
  };

  const handleGradeChange = async (studentId: string, score: string) => {
    const numScore = parseInt(score);
    if (isNaN(numScore)) return;
    const { error } = await supabase.from('grades').upsert({
      student_id: studentId, subject_id: selectedSubjectId, class_id: selectedClassId,
      session: CURRENT_SESSION, term: CURRENT_TERM, score: numScore
    }, { onConflict: 'student_id,subject_id,session,term' });
    if (!error) {
      setGrades(prev => {
        const existing = prev.find(g => g.student_id === studentId);
        if (existing) return prev.map(g => g.student_id === studentId ? { ...g, score: numScore } : g);
        return [...prev, { id: 'temp', student_id: studentId, subject_id: selectedSubjectId, score: numScore }];
      });
    }
  };

  const downloadGradeTemplate = () => {
    if (!students.length) return alert("No students in this class.");
    const csvData = students.map(s => ({
      Student_ID: s.id, Student_Name: s.name, Score: grades.find(g => g.student_id === s.id)?.score || ''
    }));
    const csv = Papa.unparse(csvData);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.setAttribute('download', 'Grades_Template.csv');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleBulkGradeUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    Papa.parse(file, {
      header: true,
      complete: async (results: any) => {
        const rows = results.data;
        let count = 0;
        for (const row of rows) {
          if (!row.Student_ID || !row.Score) continue;
          await supabase.from('grades').upsert({
            student_id: row.Student_ID, subject_id: selectedSubjectId, class_id: selectedClassId,
            session: CURRENT_SESSION, term: CURRENT_TERM, score: parseInt(row.Score)
          }, { onConflict: 'student_id,subject_id,session,term' });
          count++;
        }
        setUploading(false);
        alert(`Uploaded ${count} grades.`);
        fetchClassData();
      }
    });
  };

  return (
    <div className="space-y-6">
      <Tabs defaultValue="grading">
        <TabsList>
          <TabsTrigger value="grading">Grade Entry</TabsTrigger>
          {managedClass && <TabsTrigger value="myclass">My Class ({managedClass.name})</TabsTrigger>}
        </TabsList>
        <TabsContent value="grading">
           <Card>
            <CardHeader><CardTitle>Subject Grading</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2"><Label>Subject</Label>
                  <Select value={selectedSubjectId} onValueChange={setSelectedSubjectId}>
                    <SelectTrigger><SelectValue placeholder="Select Subject" /></SelectTrigger>
                    <SelectContent>{subjects.filter(s => allowedSubjectIds.includes(s.id)).map(s => <SelectItem key={s.id} value={s.id}>{s.name}</SelectItem>)}</SelectContent>
                  </Select>
                </div>
                <div className="space-y-2"><Label>Class</Label>
                  <Select value={selectedClassId} onValueChange={setSelectedClassId}>
                    <SelectTrigger><SelectValue placeholder="Select Class" /></SelectTrigger>
                    <SelectContent>{classes.filter(c => c.subjects.includes(selectedSubjectId)).map(c => <SelectItem key={c.id} value={c.id}>{c.name}</SelectItem>)}</SelectContent>
                  </Select>
                </div>
              </div>
              {selectedClassId && (
                <div className="mt-6">
                  <div className="flex justify-between items-center mb-4 p-4 bg-slate-50 rounded border">
                    <div><h3 className="font-bold text-sm">Bulk Operations</h3><p className="text-xs text-slate-500">Download &rarr; Fill &rarr; Upload</p></div>
                    <div className="flex gap-2">
                      <Button variant="outline" size="sm" onClick={downloadGradeTemplate}><Download className="w-4 h-4 mr-2" /> Template</Button>
                      <div className="relative">
                        <input type="file" id="gradeUpload" className="hidden" accept=".csv" onChange={handleBulkGradeUpload} disabled={uploading} />
                        <Button size="sm" asChild disabled={uploading}>
                          <label htmlFor="gradeUpload" className="cursor-pointer">{uploading ? <Loader2 className="w-4 h-4 animate-spin" /> : <UploadCloud className="w-4 h-4 mr-2" />} Upload</label>
                        </Button>
                      </div>
                    </div>
                  </div>
                  {loading ? <div className="text-center py-8">Loading...</div> : (
                    <Table>
                      <TableHeader><TableRow><TableHead>Student Name</TableHead><TableHead>Score (0-100)</TableHead></TableRow></TableHeader>
                      <TableBody>
                        {students.map(student => {
                          const grade = grades.find(g => g.student_id === student.id);
                          return (
                            <TableRow key={student.id}>
                              <TableCell className="font-medium">{student.name}</TableCell>
                              <TableCell><Input type="number" max="100" className="w-24" defaultValue={grade?.score || ''} onBlur={(e) => handleGradeChange(student.id, e.target.value)} /></TableCell>
                            </TableRow>
                          );
                        })}
                      </TableBody>
                    </Table>
                  )}
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>
        {managedClass && (
          <TabsContent value="myclass">
            <Card className="border-t-4 border-blue-500">
              <CardHeader><CardTitle>Form Master: {managedClass.name}</CardTitle></CardHeader>
              <CardContent>
                <Table>
                  <TableHeader><TableRow><TableHead>Name</TableHead><TableHead>Username</TableHead></TableRow></TableHeader>
                  <TableBody>
                    {students.map(s => (
                       <TableRow key={s.id}><TableCell>{s.name}</TableCell><TableCell>{s.username}</TableCell></TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </TabsContent>
        )}
      </Tabs>
    </div>
  );
};

const AdminDashboard = ({ 
  data, refreshData 
}: { 
  data: { users: User[], classes: Class[], subjects: Subject[] }, 
  refreshData: () => void 
}) => {
  const [uploading, setUploading] = useState(false);
  
  // Forms State
  const [newStudentName, setNewStudentName] = useState('');
  const [newStudentClass, setNewStudentClass] = useState('');
  const [newTeacherName, setNewTeacherName] = useState('');
  const [newTeacherSub, setNewTeacherSub] = useState('');
  const [assignTeacherId, setAssignTeacherId] = useState('');
  const [assignClassId, setAssignClassId] = useState('');
  const [extraTeacherId, setExtraTeacherId] = useState('');
  const [extraSubjectId, setExtraSubjectId] = useState('');
  const [baseClass, setBaseClass] = useState('');
  const [armLetter, setArmLetter] = useState('');
  const [newSubName, setNewSubName] = useState('');
  const [newSubClass, setNewSubClass] = useState('');
  const [addToAllArms, setAddToAllArms] = useState(false);

  // --- ACTIONS ---

  const handleBulkStudentUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    Papa.parse(file, {
      header: true,
      complete: async (results: any) => {
        const rows = results.data;
        let count = 0;
        for (const row of rows) {
          if (!row.Name || !row.Username || !row.ClassName) continue;
          const targetClass = data.classes.find(c => c.name.toLowerCase() === row.ClassName.trim().toLowerCase());
          if (targetClass) {
            await supabase.from('users').insert({
              name: row.Name, username: row.Username, password: row.Password || '123456',
              role: 'student', current_class_id: targetClass.id
            });
            count++;
          }
        }
        setUploading(false);
        alert(`Imported ${count} students.`);
        refreshData();
      }
    });
  };

  const handleAddStudent = async () => {
    if(!newStudentName || !newStudentClass) return;
    await supabase.from('users').insert({
      name: newStudentName, username: newStudentName.toLowerCase().replace(/\s/g, ''), password: 'password',
      role: 'student', current_class_id: newStudentClass
    });
    setNewStudentName('');
    alert("Student Registered");
    refreshData();
  };

  const handleAddTeacher = async () => {
    if(!newTeacherName || !newTeacherSub) return;
    await supabase.from('users').insert({
      name: newTeacherName, username: newTeacherName.toLowerCase().split(' ')[0], password: 'password',
      role: 'teacher', primary_subject_id: newTeacherSub
    });
    setNewTeacherName('');
    alert("Teacher Added");
    refreshData();
  };

  const handleAssignFormMaster = async () => {
    if(!assignTeacherId || !assignClassId) return;
    await supabase.from('users').update({ managed_class_id: null }).eq('managed_class_id', assignClassId);
    await supabase.from('users').update({ managed_class_id: assignClassId }).eq('id', assignTeacherId);
    alert("Form Master Assigned");
    refreshData();
  };

  const handleAssignExtraSubject = async () => {
    if(!extraTeacherId || !extraSubjectId) return;
    const teacher = data.users.find(u => u.id === extraTeacherId);
    if (!teacher) return;
    const currentExtras = teacher.additional_subject_ids || [];
    const newExtras = [...new Set([...currentExtras, extraSubjectId])];
    await supabase.from('users').update({ additional_subject_ids: newExtras }).eq('id', extraTeacherId);
    alert("Subject Assigned");
    refreshData();
  };

  const handleAddClass = async () => {
    if(!baseClass || !armLetter) return;
    const name = `${baseClass}${armLetter}`;
    const level = baseClass