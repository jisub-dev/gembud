export interface Evaluation {
  id: number;
  roomId: number;
  evaluatorId: number;
  evaluatedId: number;
  mannerScore: number;
  skillScore: number;
  communicationScore: number;
  averageScore: number;
  createdAt: string;
}

export interface EvaluationRequest {
  evaluatedId: number;
  mannerScore: number;
  skillScore: number;
  communicationScore: number;
}

export interface TemperatureStats {
  userId: number;
  currentTemperature: number;
  evaluationCount: number;
  averageMannerScore: number;
  averageSkillScore: number;
  averageCommunicationScore: number;
}
